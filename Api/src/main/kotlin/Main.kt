import io.ktor.server.application.Application
import io.ktor.server.engine.*
import io.ktor.server.netty.*
fun main() {
    println("Iniciando el servidor")
    // Esto enciende el servidor y lo conecta con tu archivo Application.kt
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}