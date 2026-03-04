# --- Etapa 1: Construcción ---
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /app

COPY . .

RUN chmod +x gradlew
RUN ./gradlew buildFatJar --no-daemon -Dorg.gradle.jvmargs="-Xmx256m"

# --- Etapa 2: Ejecución ---
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

COPY --from=build /app/build/libs/*-all.jar /app/api-pymes.jar

EXPOSE 8080

CMD ["java", "-jar", "/app/api-pymes.jar"]
