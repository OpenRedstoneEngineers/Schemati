import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    kotlin("kapt")
    id("com.github.johnrengelman.shadow")
}

dependencies {
    implementation(project(":schemati-core"))
    // TODO: remove dumb dependency
    implementation(project(":schemati-web"))
    implementation(kotlin("stdlib-jdk8"))
    implementation(group = "co.aikar", name = "acf-paper", version = "0.5.0-SNAPSHOT")

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
    kotlinOptions.jvmTarget = "1.8"
    kotlinOptions.javaParameters = true
}
