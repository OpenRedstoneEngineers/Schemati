import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    kotlin("kapt")
    id("com.gradleup.shadow")
}

dependencies {
    implementation(project(":schemati-core"))
    // TODO: remove dumb dependency
    implementation(project(":schemati-web"))
    implementation(kotlin("stdlib-jdk8"))
    implementation(group = "co.aikar", name = "acf-paper", version = "0.5.1-SNAPSHOT")

    compileOnly(group = "org.spigotmc", name = "spigot-api", version = "1.16.1-R0.1-SNAPSHOT")
    compileOnly(group = "com.sk89q.worldedit", name = "worldedit-bukkit", version = "7.2.0-SNAPSHOT")
}

tasks.shadowJar {
    relocate("co.aikar.commands", "schemati.acf")
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
