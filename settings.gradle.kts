pluginManagement {
    repositories {
        maven("https://maven.fabricmc.net/") { name = "Fabric" }
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        maven("https://maven.fabricmc.net/") { name = "Fabric" }
        maven("https://libraries.minecraft.net/") { name = "Mojang" }
        mavenCentral()
    }
}

rootProject.name = "offworld"

include(":offworld")
include(":offworld-fabric")