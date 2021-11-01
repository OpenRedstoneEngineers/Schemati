import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    kotlin("kapt")
    id("com.github.johnrengelman.shadow")
}

val ktorVersion = "1.6.4"

dependencies {
    implementation(project(":schemati-core"))
    implementation(kotlin("stdlib-jdk8"))

    // delete api
    api(group = "io.ktor", name = "ktor-server-netty", version = ktorVersion)
    implementation(group = "io.ktor", name = "ktor-auth", version = ktorVersion)
    implementation(group = "io.ktor", name = "ktor-client-apache", version = ktorVersion)
    implementation(group = "io.ktor", name = "ktor-html-builder", version = ktorVersion)

    implementation(group = "org.jetbrains", name = "kotlin-css", version = "1.0.0-pre.104-kotlin-1.3.72")

    implementation(group = "com.fasterxml.jackson.module", name = "jackson-module-kotlin", version = "2.11.0")

    implementation(group = "mysql", name = "mysql-connector-java", version = "8.0.20")
    implementation(group = "com.vladsch.kotlin-jdbc", name = "kotlin-jdbc", version = "0.5.0")

    // idk? only for testing or something, cuz the server probably provides it anyway
//    implementation("org.slf4j:slf4j-simple:1.7.32")
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
    kotlinOptions.javaParameters = true
}
