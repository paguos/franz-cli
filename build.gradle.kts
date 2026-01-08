plugins {
    kotlin("jvm") version "1.9.22"
    application
    id("org.graalvm.buildtools.native") version "0.10.0"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "dev.franz"
version = "0.1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.github.ajalt.clikt:clikt:4.2.2")
    
    // Kafka client
    implementation("org.apache.kafka:kafka-clients:4.1.0")
    
    // Testing
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
    testImplementation("io.mockk:mockk:1.13.9")
    testImplementation("org.assertj:assertj-core:3.25.1")
    
    // Testcontainers for integration tests
    testImplementation("org.testcontainers:kafka:1.20.4")
    testImplementation("org.testcontainers:junit-jupiter:1.20.4")
}

application {
    mainClass.set("dev.franz.cli.MainKt")
}

kotlin {
    jvmToolchain(21)
}

graalvmNative {
    binaries {
        named("main") {
            imageName.set("franz")
            mainClass.set("dev.franz.cli.MainKt")
            buildArgs.add("--no-fallback")
            buildArgs.add("-H:+ReportExceptionStackTraces")
        }
    }
    toolchainDetection.set(false)
}

tasks.test {
    useJUnitPlatform()
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "dev.franz.cli.MainKt"
    }
}
