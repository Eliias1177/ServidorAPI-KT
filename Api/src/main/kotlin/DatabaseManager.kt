import java.sql.Connection
import java.sql.DriverManager

object DatabaseManager {
    // Esto creará un archivo llamado "inventario_pymes.db" en tu proyecto
    private const val URL = "jdbc:sqlite:inventario_pymes.db"

    // Función para obtener la conexión
    fun conectar(): Connection {
        return DriverManager.getConnection(URL)
    }

    // Función para crear la tabla si es la primera vez que corres el programa
    fun inicializarBaseDeDatos() {
        val conexion = conectar()
        val statement = conexion.createStatement()

        // Código SQL puro para crear la tabla de productos
        val sql = """
            CREATE TABLE IF NOT EXISTS productos (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                nombre TEXT NOT NULL,
                descripcion TEXT,
                precio REAL NOT NULL,
                cantidadEnStock INTEGER NOT NULL
            )
        """.trimIndent()

        statement.execute(sql)

        // Siempre es buena práctica cerrar las conexiones
        statement.close()
        conexion.close()

        println("Base de datos lista. Tabla 'productos' verificada.")
    }
}