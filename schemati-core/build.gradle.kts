import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    kotlin("kapt")
    id("com.gradleup.shadow")
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    compileOnly(group = "org.spigotmc", name = "spigot-api", version = "1.16.1-R0.1-SNAPSHOT")
    compileOnly(group = "com.sk89q.worldedit", name = "worldedit-bukkit", version = "7.2.0-SNAPSHOT")
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
