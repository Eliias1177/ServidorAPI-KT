plugins {

        kotlin("jvm") version "1.9.22"
        kotlin("plugin.serialization") version "1.9.22" // Agrega esta línea
        application
    }

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    implementation("io.ktor:ktor-server-core-jvm:2.3.8")
    implementation("io.ktor:ktor-server-netty-jvm:2.3.8")
    implementation("ch.qos.logback:logback-classic:1.4.14")
    implementation("io.ktor:ktor-server-config-yaml:2.3.8")
    testImplementation("io.ktor:ktor-server-test-host-jvm:2.3.8")
    implementation("org.xerial:sqlite-jdbc:3.45.1.0")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:2.3.8")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:2.3.8")

    // Esta es la línea corregida:
    implementation("io.ktor:ktor-server-swagger:2.3.8")
}

kotlin {
    jvmToolchain(23)
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}