import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.swagger.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import java.sql.Statement

@Serializable
data class Producto(
    val id: Int? = null,
    val nombre: String,
    val descripcion: String,
    val precio: Double,
    val cantidadEnStock: Int
)

fun Application.module() {
    DatabaseManager.inicializarBaseDeDatos()

    install(ContentNegotiation) {
        json()
    }

    routing {
        // La vista de Swagger
        swaggerUI(path = "docs", swaggerFile = "src/main/resources/openapi/documentation.yaml")

        // 1. "/" - Muestra docs (Redirige automáticamente a la pantalla de Swagger)
        get("/") {
            call.respondRedirect("/docs")
        }

        // Agrupamos todo bajo /api/v1/ como pide el profe
        route("/api/v1") {

            // 2. "/api/v1/health" - Chequeo de si responde el servidor
            get("/health") {
                call.respondText("OK - El servidor está funcionando correctamente", status = HttpStatusCode.OK)
            }

            // 3. "/api/v1/login" - Iniciar sesión y retorna una API key
            post("/login") {
                // Simulamos un login devolviendo un texto en JSON con la llave
                call.respondText("""{"api_key": "clave_secreta_pymes_777"}""", ContentType.Application.Json, status = HttpStatusCode.OK)
            }

            // 4. Tus rutas (El profe puso usuarios, nosotros tenemos productos con ID autoincrementable)
            route("/productos") {

                get {
                    val listaProductos = mutableListOf<Producto>()
                    val conexion = DatabaseManager.conectar()
                    val statement = conexion.createStatement()
                    val resultSet = statement.executeQuery("SELECT * FROM productos")

                    while (resultSet.next()) {
                        val producto = Producto(
                            id = resultSet.getInt("id"),
                            nombre = resultSet.getString("nombre"),
                            descripcion = resultSet.getString("descripcion"),
                            precio = resultSet.getDouble("precio"),
                            cantidadEnStock = resultSet.getInt("cantidadEnStock")
                        )
                        listaProductos.add(producto)
                    }

                    resultSet.close()
                    statement.close()
                    conexion.close()
                    call.respond(listaProductos)
                }

                get("/{id}") {
                    val id = call.parameters["id"]?.toIntOrNull()

                    if (id == null) {
                        call.respondText("ID inválido", status = HttpStatusCode.BadRequest)
                    } else {
                        val conexion = DatabaseManager.conectar()
                        val query = "SELECT * FROM productos WHERE id = ?"
                        val statement = conexion.prepareStatement(query)
                        statement.setInt(1, id)
                        val resultSet = statement.executeQuery()

                        if (resultSet.next()) {
                            val producto = Producto(
                                id = resultSet.getInt("id"),
                                nombre = resultSet.getString("nombre"),
                                descripcion = resultSet.getString("descripcion"),
                                precio = resultSet.getDouble("precio"),
                                cantidadEnStock = resultSet.getInt("cantidadEnStock")
                            )
                            call.respond(producto)
                        } else {
                            call.respondText("Producto no encontrado", status = HttpStatusCode.NotFound)
                        }

                        resultSet.close()
                        statement.close()
                        conexion.close()
                    }
                }

                post {
                    val nuevoProducto = call.receive<Producto>()
                    val conexion = DatabaseManager.conectar()
                    val query = "INSERT INTO productos (nombre, descripcion, precio, cantidadEnStock) VALUES (?, ?, ?, ?)"
                    val statement = conexion.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)

                    statement.setString(1, nuevoProducto.nombre)
                    statement.setString(2, nuevoProducto.descripcion)
                    statement.setDouble(3, nuevoProducto.precio)
                    statement.setInt(4, nuevoProducto.cantidadEnStock)
                    statement.executeUpdate()

                    val keys = statement.generatedKeys
                    var idGenerado = 0
                    if (keys.next()) { idGenerado = keys.getInt(1) }

                    statement.close()
                    conexion.close()
                    call.respondText("Producto guardado con éxito. ID asignado: $idGenerado", status = HttpStatusCode.Created)
                }

                put("/{id}") {
                    val id = call.parameters["id"]?.toIntOrNull()
                    if (id == null) {
                        call.respondText("ID inválido", status = HttpStatusCode.BadRequest)
                    } else {
                        val productoActualizado = call.receive<Producto>()
                        val conexion = DatabaseManager.conectar()
                        val query = "UPDATE productos SET nombre = ?, descripcion = ?, precio = ?, cantidadEnStock = ? WHERE id = ?"
                        val statement = conexion.prepareStatement(query)

                        statement.setString(1, productoActualizado.nombre)
                        statement.setString(2, productoActualizado.descripcion)
                        statement.setDouble(3, productoActualizado.precio)
                        statement.setInt(4, productoActualizado.cantidadEnStock)
                        statement.setInt(5, id)

                        val filasAfectadas = statement.executeUpdate()
                        statement.close()
                        conexion.close()

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
                        val query = "DELETE FROM productos WHERE id = ?"
                        val statement = conexion.prepareStatement(query)
                        statement.setInt(1, id)

                        val filasAfectadas = statement.executeUpdate()
                        statement.close()
                        conexion.close()

                        if (filasAfectadas > 0) call.respondText("Producto eliminado", status = HttpStatusCode.OK)
                        else call.respondText("Producto no encontrado", status = HttpStatusCode.NotFound)
                    }
                }
            }
        }
    }
}