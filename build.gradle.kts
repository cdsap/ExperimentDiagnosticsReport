import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "2.2.20"
    application
    id("io.github.cdsap.fatbinary") version "1.0"
    id("org.jlleitschuh.gradle.ktlint") version "13.1.0"
}

group = "io.github.cdsap"
version = "0.1.0"

repositories {
    mavenCentral()
}

fatBinary {
    // Plugin appends "Kt" for Kotlin entrypoints; pass the file stem like BER does.
    mainClass = "io.github.cdsap.experimentdiagnostics.Main"
    name = "experiment-diagnostics-report"
}

dependencies {
    implementation("com.github.ajalt.clikt:clikt:5.1.0")
    implementation("com.google.code.gson:gson:2.13.2")
    implementation("org.nield:kotlin-statistics:1.2.1")
    implementation("io.ktor:ktor-client-core:2.3.12")
    implementation("io.ktor:ktor-client-cio:2.3.12")
    implementation("io.ktor:ktor-client-content-negotiation:2.3.12")
    implementation("io.ktor:ktor-serialization-gson:2.3.12")
    testImplementation(kotlin("test-junit5"))
    testImplementation(platform("org.junit:junit-bom:5.10.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}

application {
    mainClass.set("io.github.cdsap.experimentdiagnostics.MainKt")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}
