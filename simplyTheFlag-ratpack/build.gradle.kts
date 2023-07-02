import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.8.21"
    application
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.amazonaws:aws-java-sdk:1.12.496")
    implementation("software.amazon.awssdk:ssm:2.20.94")
    api("org.testcontainers:localstack:1.18.3")
    api("com.fasterxml.jackson.module:jackson-module-kotlin")
    api("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    implementation(project(mapOf("path" to ":simplyTheFlag")))

    val kotestVersion = "5.4.1"
    testImplementation("io.kotest:kotest-runner-junit5:$kotestVersion")
    testImplementation("io.kotest:kotest-assertions-core:$kotestVersion")
    testImplementation("io.kotest:kotest-framework-datatest:$kotestVersion")

    testImplementation("org.testcontainers:testcontainers:1.17.6")

    implementation("io.ratpack:ratpack-core:1.9.0")
    api("io.netty:netty-resolver-dns-native-macos:4.1.92.Final:osx-aarch_64")
    testImplementation("io.ratpack:ratpack-test:1.9.0")
    testImplementation("io.ratpack:ratpack-guice:1.9.0")
    testImplementation("com.google.inject:guice:5.1.0")
}

tasks.withType<Test>{
    useJUnitPlatform()
    maxHeapSize = "1g"

    testLogging {
        events(TestLogEvent.FAILED, TestLogEvent.STANDARD_ERROR, TestLogEvent.SKIPPED)

        exceptionFormat = TestExceptionFormat.FULL
        showExceptions = true
        showCauses = true
        showStackTraces = true
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
}