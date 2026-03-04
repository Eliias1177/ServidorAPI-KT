# --- Etapa 1: Construcción (El Chef) ---
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /app
COPY . .
RUN chmod +x gradlew
RUN ./gradlew buildFatJar --no-daemon -Dorg.gradle.jvmargs="-Xmx256m"

# --- Etapa 2: Ejecución (El Mesero) ---
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
EXPOSE 8080

# 1. Copiamos el programa ya compilado
COPY --from=build /app/build/libs/*-all.jar /app/api-pymes.jar

# 2. ¡Copiamos tu base de datos para que no se pierdan tus productos!
COPY --from=build /app/inventario_pymes.db /app/inventario_pymes.db

# Encendemos la API
CMD ["java", "-jar", "/app/api-pymes.jar"]
