import java.security.MessageDigest
import java.sql.Connection
import java.sql.DriverManager

object DatabaseManager {
    private const val URL = "jdbc:sqlite:inventario_pymes.db"

    fun conectar(): Connection = DriverManager.getConnection(URL)

    fun inicializarBaseDeDatos() {
        val conexion = conectar()
        val statement = conexion.createStatement()

        // Tabla de productos (igual que antes)
        statement.execute("""
            CREATE TABLE IF NOT EXISTS productos (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                nombre TEXT NOT NULL,
                descripcion TEXT,
                precio REAL NOT NULL,
                cantidadEnStock INTEGER NOT NULL
            )
        """.trimIndent())

        // 🔐 Nueva tabla de usuarios
        statement.execute("""
            CREATE TABLE IF NOT EXISTS usuarios (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                username TEXT NOT NULL UNIQUE,
                password TEXT NOT NULL
            )
        """.trimIndent())

        statement.close()
        conexion.close()
        println("✅ Base de datos lista. Tablas 'productos' y 'usuarios' verificadas.")
    }

    // 🔒 Convierte la contraseña en un hash SHA-256 (nunca guardamos texto plano)
    fun hashPassword(password: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(password.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    // Crea un usuario nuevo — retorna false si el username ya existe
    fun crearUsuario(username: String, password: String): Boolean {
        return try {
            val conexion = conectar()
            val query = "INSERT INTO usuarios (username, password) VALUES (?, ?)"
            val statement = conexion.prepareStatement(query)
            statement.setString(1, username)
            statement.setString(2, hashPassword(password))
            statement.executeUpdate()
            statement.close()
            conexion.close()
            true
        } catch (e: Exception) {
            false // El username ya existe (UNIQUE constraint)
        }
    }

    // Busca un usuario por username+password — retorna su ID si existe, null si no
    fun buscarUsuario(username: String, password: String): Int? {
        val conexion = conectar()
        val query = "SELECT id FROM usuarios WHERE username = ? AND password = ?"
        val statement = conexion.prepareStatement(query)
        statement.setString(1, username)
        statement.setString(2, hashPassword(password))
        val result = statement.executeQuery()
        val id = if (result.next()) result.getInt("id") else null
        result.close()
        statement.close()
        conexion.close()
        return id
    }
}
