plugins {
    kotlin("jvm") version "1.4.20" apply false
    kotlin("kapt") version "1.4.20" apply false
    id("com.github.johnrengelman.shadow") version "2.0.4" apply false
}


allprojects {
    group = "org.openredstone.schemati"
    version = "1.1"

    repositories {
        jcenter()
        mavenCentral()
        maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
        maven("https://oss.sonatype.org/content/groups/public/")
        maven("https://maven.enginehub.org/repo/")
        maven("https://dl.bintray.com/adhesivee/oauth2-server")
        maven("https://repo.aikar.co/content/groups/aikar/")
    }
}
