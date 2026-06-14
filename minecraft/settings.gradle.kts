pluginManagement {
    val kotlinVersion = providers.gradleProperty("kotlin_version").get()
    val fabricLoomVersion = providers.gradleProperty("fabric_loom_version").get()
    repositories {
        maven("https://maven.fabricmc.net/")
        gradlePluginPortal()
        mavenCentral()
    }
    plugins {
        id("org.jetbrains.kotlin.jvm") version kotlinVersion
        id("net.fabricmc.fabric-loom") version fabricLoomVersion
    }
}

rootProject.name = "offworld"

include(
    ":content",
    ":genesis", ":tech", ":menu",
    ":fabric:genesis", ":fabric:tech", ":fabric:menu",
    ":neoforge:genesis", ":neoforge:tech", ":neoforge:menu",
)
