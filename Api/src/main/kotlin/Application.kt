import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.swagger.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import java.sql.Statement
import java.util.Date

// --- Modelos de datos ---

// ✅ Lo que el usuario ENVÍA (sin id — el servidor lo asigna solo)
@Serializable
data class ProductoRequest(
    val nombre: String,
    val descripcion: String,
    val precio: Double,
    val cantidadEnStock: Int
)

// ✅ Lo que el servidor RESPONDE (con id asignado por la BD)
@Serializable
data class Producto(
    val id: Int,
    val nombre: String,
    val descripcion: String,
    val precio: Double,
    val cantidadEnStock: Int
)

@Serializable
data class LoginRequest(val username: String, val password: String)

@Serializable
data class RegisterRequest(val username: String, val password: String)

// --- Configuración JWT ---
// ⚠️ En producción real esto debería ir en variables de entorno
private const val JWT_SECRET   = "mi_secreto_super_seguro_pymes_2024"
private const val JWT_ISSUER   = "api-pymes"
private const val JWT_AUDIENCE = "pymes-users"

fun Application.module() {
    DatabaseManager.inicializarBaseDeDatos()

    // Plugin de serialización JSON
    install(ContentNegotiation) { json() }

    // 🔐 Instalamos el sistema de autenticación JWT
    install(Authentication) {
        jwt("auth-jwt") {
            realm = "API Pymes"
            verifier(
                JWT.require(Algorithm.HMAC256(JWT_SECRET))
                    .withIssuer(JWT_ISSUER)
                    .withAudience(JWT_AUDIENCE)
                    .build()
            )
            validate { credential ->
                // El token es válido si tiene el campo "username"
                if (credential.payload.getClaim("username").asString() != null)
                    JWTPrincipal(credential.payload)
                else null
            }
            challenge { _, _ ->
                // Respuesta cuando el token es inválido o no existe
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Token inválido o expirado. Haz login primero."))
            }
        }
    }

    routing {
        swaggerUI(path = "docs", swaggerFile = "openapi/documentation.yaml")

        get("/") { call.respondRedirect("/docs") }

        route("/api/v1") {

            // ✅ PÚBLICO: Chequeo del servidor
            get("/health") {
                call.respondText("OK - El servidor está funcionando correctamente", status = HttpStatusCode.OK)
            }

            // ✅ PÚBLICO: Crear cuenta nueva
            post("/register") {
                val body = call.receive<RegisterRequest>()
                if (body.username.isBlank() || body.password.isBlank()) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Username y password son obligatorios"))
                    return@post
                }
                val creado = DatabaseManager.crearUsuario(body.username, body.password)
                if (creado) {
                    call.respond(HttpStatusCode.Created, mapOf("mensaje" to "Usuario '${body.username}' creado correctamente"))
                } else {
                    call.respond(HttpStatusCode.Conflict, mapOf("error" to "El username '${body.username}' ya existe"))
                }
            }

            // ✅ PÚBLICO: Login — retorna el JWT real
            post("/login") {
                val body = call.receive<LoginRequest>()
                val userId = DatabaseManager.buscarUsuario(body.username, body.password)

                if (userId != null) {
                    // 🎟️ Generamos el token con 24 horas de vigencia
                    val token = JWT.create()
                        .withIssuer(JWT_ISSUER)
                        .withAudience(JWT_AUDIENCE)
                        .withClaim("username", body.username)
                        .withClaim("userId", userId)
                        .withExpiresAt(Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000))
                        .sign(Algorithm.HMAC256(JWT_SECRET))

                    call.respond(mapOf("token" to token))
                } else {
                    call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Usuario o contraseña incorrectos"))
                }
            }

            // 🔒 PROTEGIDO: Todas las rutas de productos requieren token
            authenticate("auth-jwt") {

                route("/productos") {

                    get {
                        val listaProductos = mutableListOf<Producto>()
                        val conexion = DatabaseManager.conectar()
                        val statement = conexion.createStatement()
                        val resultSet = statement.executeQuery("SELECT * FROM productos")

                        while (resultSet.next()) {
                            listaProductos.add(
                                Producto(
                                    id = resultSet.getInt("id"),
                                    nombre = resultSet.getString("nombre"),
                                    descripcion = resultSet.getString("descripcion"),
                                    precio = resultSet.getDouble("precio"),
                                    cantidadEnStock = resultSet.getInt("cantidadEnStock")
                                )
                            )
                        }
                        resultSet.close(); statement.close(); conexion.close()
                        call.respond(listaProductos)
                    }

                    get("/{id}") {
                        val id = call.parameters["id"]?.toIntOrNull()
                        if (id == null) {
                            call.respondText("ID inválido", status = HttpStatusCode.BadRequest)
                        } else {
                            val conexion = DatabaseManager.conectar()
                            val statement = conexion.prepareStatement("SELECT * FROM productos WHERE id = ?")
                            statement.setInt(1, id)
                            val resultSet = statement.executeQuery()

                            if (resultSet.next()) {
                                call.respond(Producto(
                                    id = resultSet.getInt("id"),
                                    nombre = resultSet.getString("nombre"),
                                    descripcion = resultSet.getString("descripcion"),
                                    precio = resultSet.getDouble("precio"),
                                    cantidadEnStock = resultSet.getInt("cantidadEnStock")
                                ))
                            } else {
                                call.respondText("Producto no encontrado", status = HttpStatusCode.NotFound)
                            }
                            resultSet.close(); statement.close(); conexion.close()
                        }
                    }

                    post {
                        val nuevoProducto = call.receive<ProductoRequest>()
                        val conexion = DatabaseManager.conectar()
                        val statement = conexion.prepareStatement(
                            "INSERT INTO productos (nombre, descripcion, precio, cantidadEnStock) VALUES (?, ?, ?, ?)",
                            Statement.RETURN_GENERATED_KEYS
                        )
                        statement.setString(1, nuevoProducto.nombre)
                        statement.setString(2, nuevoProducto.descripcion)
                        statement.setDouble(3, nuevoProducto.precio)
                        statement.setInt(4, nuevoProducto.cantidadEnStock)
                        statement.executeUpdate()

                        val keys = statement.generatedKeys
                        val idGenerado = if (keys.next()) keys.getInt(1) else 0
                        statement.close(); conexion.close()
                        call.respondText("Producto guardado con éxito. ID asignado: $idGenerado", status = HttpStatusCode.Created)
                    }

                    put("/{id}") {
                        val id = call.parameters["id"]?.toIntOrNull()
                        if (id == null) {
                            call.respondText("ID inválido", status = HttpStatusCode.BadRequest)
                        } else {
                            val productoActualizado = call.receive<ProductoRequest>()
                            val conexion = DatabaseManager.conectar()
                            val statement = conexion.prepareStatement(
                                "UPDATE productos SET nombre = ?, descripcion = ?, precio = ?, cantidadEnStock = ? WHERE id = ?"
                            )
                            statement.setString(1, productoActualizado.nombre)
                            statement.setString(2, productoActualizado.descripcion)
                            statement.setDouble(3, productoActualizado.precio)
                            statement.setInt(4, productoActualizado.cantidadEnStock)
                            statement.setInt(5, id)

                            val filasAfectadas = statement.executeUpdate()
                            statement.close(); conexion.close()

                            if (filasAfectadas > 0) call.respondText("Producto actualizado", status = HttpStatusCode.OK)
                            else call.respondText("Producto no encontrado", status = HttpStatusCode.NotFound)
                        }
                    }

                    delete("/{id}") {
                        val id = call.parameters["id"]?.toIntOrNull()
                        if (id == null) {
                            call.respondText("ID inválido", status = HttpStatusCode.BadRequest)
                        } else {
                            val conexion = DatabaseManager.conectar()
                            val statement = conexion.prepareStatement("DELETE FROM productos WHERE id = ?")
                            statement.setInt(1, id)
                            val filasAfectadas = statement.executeUpdate()
                            statement.close(); conexion.close()

                            if (filasAfectadas > 0) call.respondText("Producto eliminado", status = HttpStatusCode.OK)
                            else call.respondText("Producto no encontrado", status = HttpStatusCode.NotFound)
                        }
                    }
                }
            }
        }
    }
}
