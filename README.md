# API Gestion de PYMES

API REST desarrollada con Kotlin + Ktor para gestión de inventario de productos, con autenticación JWT y base de datos SQLite. Desplegada con Docker en Render.

---

## Deploy

- **API en vivo:** [Tu URL de Render aquí]
- **Documentación Swagger:** [Tu URL de Render aquí]/docs
- **Imagen Docker Hub:** https://hub.docker.com/r/tu_usuario/api-pymes

---

## Tecnologías

- Kotlin + Ktor 2.3.8
- SQLite + JDBC
- JWT (JSON Web Tokens)
- Swagger UI
- Docker
- Render (hosting)

---

## Endpoints

### Publicos (sin token)

| Método | Ruta | Descripción |
|--------|------|-------------|
| GET | `/api/v1/health` | Verifica que el servidor esté activo |
| POST | `/api/v1/register` | Crea una cuenta nueva |
| POST | `/api/v1/login` | Inicia sesión y obtiene el token JWT |

### Protegidos (requieren token JWT)

| Método | Ruta | Descripción |
|--------|------|-------------|
| GET | `/api/v1/productos` | Lista todos los productos |
| GET | `/api/v1/productos/{id}` | Obtiene un producto por ID |
| POST | `/api/v1/productos` | Crea un producto nuevo |
| PUT | `/api/v1/productos/{id}` | Edita un producto existente |
| DELETE | `/api/v1/productos/{id}` | Elimina un producto |

---

## Como usar la autenticación

1. **Crea tu cuenta** en `POST /api/v1/register`:
```json
{
  "username": "elias",
  "password": "tu_password"
}
```

2. **Inicia sesión** en `POST /api/v1/login` y copia el token:
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

3. **Agrega el header** en cada request a productos:
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

El token tiene una vigencia de 24 horas.

---

## Ejemplo de producto

```json
{
  "nombre": "Laptop Dell",
  "descripcion": "Laptop de 15 pulgadas con 16GB RAM",
  "precio": 12500.00,
  "cantidadEnStock": 10
}
```

El campo `id` es asignado automáticamente por la base de datos — no se envía al crear o editar.

---

## Correr con Docker

```bash
# Construir la imagen
docker build -t api-pymes .

# Correr el contenedor
docker run -p 8080:8080 api-pymes
```

La API estará disponible en `http://localhost:8080`

---

## Estructura del proyecto

```
Api/
├── src/main/kotlin/
│   ├── Application.kt      # Rutas y lógica principal
│   ├── DatabaseManager.kt  # Conexión y operaciones en SQLite
│   └── Main.kt             # Punto de entrada del servidor
├── src/main/resources/
│   └── openapi/
│       └── documentation.yaml  # Definición Swagger
├── Dockerfile
├── build.gradle.kts
└── README.md
```

---

## Autor

Elías — Proyecto escolar de API REST con Kotlin y Ktor.
