plugins {
    kotlin("jvm") version "1.9.22"
    kotlin("plugin.serialization") version "1.9.22"
    application
    id("io.ktor.plugin") version "2.3.8" // ← Necesario para buildFatJar
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

val exposedVersion = "0.44.1"
val ktorVersion = "2.3.8"

dependencies {
    // Testing
    testImplementation(kotlin("test"))
    testImplementation("io.ktor:ktor-server-test-host-jvm:$ktorVersion")

    // Ktor - Servidor
    implementation("io.ktor:ktor-server-core-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-netty-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-config-yaml:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-swagger:$ktorVersion")

    // Ktor - Seguridad JWT 🔐
    implementation("io.ktor:ktor-server-auth:$ktorVersion")
    implementation("io.ktor:ktor-server-auth-jwt:$ktorVersion")

    // Base de datos - Exposed + SQLite
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("org.xerial:sqlite-jdbc:3.45.1.0")

    // Logs
    implementation("ch.qos.logback:logback-classic:1.4.14")
}

// ← Solo UN bloque kotlin, con Java 21
kotlin {
    jvmToolchain(21)
}

application {
    mainClass.set("MainKt")
}

tasks.test {
    useJUnitPlatform()
}
