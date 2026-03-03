# 1. Usamos la imagen oficial de Java 21 (la misma que usas en tu compu)
FROM eclipse-temurin:21-jdk-jammy

# 2. Creamos la carpeta donde vivirá nuestro código dentro del contenedor
WORKDIR /app

# 3. Copiamos todos los archivos de tu proyecto hacia adentro del contenedor
COPY . .

# 4. Le damos permisos a Gradle para que pueda ejecutar comandos
RUN chmod +x gradlew

# 5. Exponemos el puerto 8080 (donde vive tu API y tu Swagger)
EXPOSE 8080

# 6. Comando final: Encender el servidor (igualito que darle al botón Play)
CMD ["./gradlew", "run", "--no-daemon", "-Dorg.gradle.jvmargs=-Xmx256m"]
