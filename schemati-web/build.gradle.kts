import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val ktorVersion = "3.6.0"

plugins {
    kotlin("jvm")
    kotlin("kapt")
    id("com.gradleup.shadow")
    id("io.ktor.plugin") version "3.6.0"
    id("org.jetbrains.kotlin.plugin.serialization") version "2.4.0" apply true
}

dependencies {
    implementation(project(":schemati-core"))
    implementation(kotlin("stdlib-jdk8"))

    // delete api(??)
    api("io.ktor:ktor-server-cio:$ktorVersion")
    implementation("io.ktor:ktor-server-auth")
    implementation("io.ktor:ktor-server-sessions")
    implementation("io.ktor:ktor-server-html-builder")
    implementation("io.ktor:ktor-server-core")
    implementation("io.ktor:ktor-server-cio")
    implementation("io.ktor:ktor-server-status-pages")
    implementation("io.ktor:ktor-server-default-headers")
    implementation("io.ktor:ktor-client-apache5")

    implementation(group = "org.jetbrains", name = "kotlin-css", version = "1.0.0-pre.104-kotlin-1.3.72")

    implementation(group = "com.fasterxml.jackson.module", name = "jackson-module-kotlin", version = "2.11.0")

    implementation(group = "mysql", name = "mysql-connector-java", version = "8.0.20")
    implementation(group = "com.vladsch.kotlin-jdbc", name = "kotlin-jdbc", version = "0.5.0")

}

tasks.shadowJar {
    relocate("io.netty", "your.plugin.shaded.netty")
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

tasks.withType<KotlinCompile> {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
        javaParameters = true
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}
