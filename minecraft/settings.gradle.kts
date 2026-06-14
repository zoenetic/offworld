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

include(":offworld-genesis")
include(":offworld-genesis-fabric")
include(":offworld-genesis-neoforge")
include(":offworld-tech")
include(":offworld-tech-fabric")
include(":offworld-tech-neoforge")
include(":offworld-menu")
include(":offworld-menu-fabric")
include(":offworld-menu-neoforge")

project(":offworld-genesis").projectDir = file("genesis")
project(":offworld-genesis-fabric").projectDir = file("genesis/fabric")
project(":offworld-genesis-neoforge").projectDir = file("genesis/neoforge")
project(":offworld-tech").projectDir = file("tech")
project(":offworld-tech-fabric").projectDir = file("tech/fabric")
project(":offworld-tech-neoforge").projectDir = file("tech/neoforge")
project(":offworld-menu").projectDir = file("menu")
project(":offworld-menu-fabric").projectDir = file("menu/fabric")
project(":offworld-menu-neoforge").projectDir = file("menu/neoforge")
