plugins {
    id("org.jetbrains.kotlin.jvm") apply false
    id("net.fabricmc.fabric-loom") apply false
}

subprojects {
    group = providers.gradleProperty("mod_group").get()
    version = providers.gradleProperty("mod_version").get()
}