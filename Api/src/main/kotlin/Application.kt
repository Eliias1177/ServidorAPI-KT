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

@Serializable
data class ProductoRequest(
    val nombre: String,
    val descripcion: String,
    val precio: Double,
    val cantidadEnStock: Int
)

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

private const val JWT_SECRET   = "mi_secreto_super_seguro_pymes_2024"
private const val JWT_ISSUER   = "api-pymes"
private const val JWT_AUDIENCE = "pymes-users"

fun Application.module() {
    DatabaseManager.inicializarBaseDeDatos()

    install(ContentNegotiation) { json() }

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
                if (credential.payload.getClaim("username").asString() != null)
                    JWTPrincipal(credential.payload)
                else null
            }
            challenge { _, _ ->
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Token inválido o expirado. Haz login primero."))
            }
        }
    }

    routing {
        swaggerUI(path = "docs", swaggerFile = "openapi/documentation.yaml")

        get("/") { call.respondRedirect("/docs") }

        route("/api/v1") {

            get("/health") {
                call.respond(HttpStatusCode.OK, mapOf("mensaje" to "OK - El servidor está funcionando correctamente"))
            }

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

            post("/login") {
                val body = call.receive<LoginRequest>()
                val userId = DatabaseManager.buscarUsuario(body.username, body.password)

                if (userId != null) {
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
                            call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido"))
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
                                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Producto no encontrado"))
                            }
                            resultSet.close(); statement.close(); conexion.close()
                        }
                    }

                    post {
                        val nuevoProducto = call.receive<ProductoRequest>()
                        
                        if (nuevoProducto.nombre.isBlank()) return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "El nombre es obligatorio"))
                        if (nuevoProducto.precio <= 0) return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "El precio debe ser mayor a 0"))
                        if (nuevoProducto.cantidadEnStock < 0) return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "El stock no puede ser negativo"))

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
                        
                        call.respond(HttpStatusCode.Created, mapOf(
                            "mensaje" to "Producto guardado con éxito",
                            "idAsignado" to idGenerado
                        ))
                    }

                    put("/{id}") {
                        val id = call.parameters["id"]?.toIntOrNull()
                        if (id == null) return@put call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido"))

                        val productoActualizado = call.receive<ProductoRequest>()

                        if (productoActualizado.nombre.isBlank()) return@put call.respond(HttpStatusCode.BadRequest, mapOf("error" to "El nombre es obligatorio"))
                        if (productoActualizado.precio <= 0) return@put call.respond(HttpStatusCode.BadRequest, mapOf("error" to "El precio debe ser mayor a 0"))
                        if (productoActualizado.cantidadEnStock < 0) return@put call.respond(HttpStatusCode.BadRequest, mapOf("error" to "El stock no puede ser negativo"))

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

                        if (filasAfectadas > 0) call.respond(HttpStatusCode.OK, mapOf("mensaje" to "Producto actualizado"))
                        else call.respond(HttpStatusCode.NotFound, mapOf("error" to "Producto no encontrado"))
                    }

                    delete("/{id}") {
                        val id = call.parameters["id"]?.toIntOrNull()
                        if (id == null) {
                            call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido"))
                        } else {
                            val conexion = DatabaseManager.conectar()
                            val statement = conexion.prepareStatement("DELETE FROM productos WHERE id = ?")
                            statement.setInt(1, id)
                            val filasAfectadas = statement.executeUpdate()
                            statement.close(); conexion.close()

                            if (filasAfectadas > 0) call.respond(HttpStatusCode.OK, mapOf("mensaje" to "Producto eliminado"))
                            else call.respond(HttpStatusCode.NotFound, mapOf("error" to "Producto no encontrado"))
                        }
                    }
                }
            }
        }
    }
}
