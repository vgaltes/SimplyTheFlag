import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.gradle.api.tasks.testing.logging.TestExceptionFormat.*
import org.gradle.api.tasks.testing.logging.TestLogEvent.*

plugins {
    kotlin("jvm") version "1.8.21"
    application
}

dependencies {
    implementation("com.amazonaws:aws-java-sdk:1.12.496")
    implementation("software.amazon.awssdk:ssm:2.20.94")
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.8.22")
    api("org.testcontainers:localstack:1.18.3")
    api("com.fasterxml.jackson.module:jackson-module-kotlin")
    api("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")

    val kotestVersion = "5.4.1"
    testImplementation("io.kotest:kotest-runner-junit5:$kotestVersion")
    testImplementation("io.kotest:kotest-assertions-core:$kotestVersion")
    testImplementation("io.kotest:kotest-framework-datatest:$kotestVersion")

    testImplementation("org.testcontainers:testcontainers:1.17.6")
}

repositories {
    mavenCentral()
}

tasks.withType<Test>{
    useJUnitPlatform()
    maxHeapSize = "1g"

    testLogging {
        events(FAILED, STANDARD_ERROR, SKIPPED)

        exceptionFormat = FULL
        showExceptions = true
        showCauses = true
        showStackTraces = true
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
}

application {
    mainClass.set("MainKt")
}