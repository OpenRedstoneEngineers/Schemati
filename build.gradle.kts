plugins {
    kotlin("jvm") version "2.4.0" apply false
    kotlin("kapt") version "2.4.0" apply false
    id("com.gradleup.shadow") version "9.4.2" apply false
}


allprojects {
    group = "org.openredstone.schemati"
    version = "1.2"

    repositories {
        mavenCentral()
        maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
        maven("https://oss.sonatype.org/content/groups/public/")
        maven("https://maven.enginehub.org/repo/")
        maven("https://dl.bintray.com/adhesivee/oauth2-server")
        maven("https://repo.aikar.co/content/groups/aikar/")
        maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/kotlin-js-wrappers")
    }
}
