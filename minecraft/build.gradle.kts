plugins {
    // minecraft/ itself is the `offworld` module — a place for shared, loader- and
    // mod-agnostic code. Loom stays unapplied here; only the loader leaf modules use it.
    id("org.jetbrains.kotlin.jvm")
    id("net.fabricmc.fabric-loom") apply false
}

allprojects {
    group = providers.gradleProperty("mod_group").get()
    version = providers.gradleProperty("mod_version").get()
}

repositories { mavenCentral() }

kotlin { jvmToolchain(25) }

base { archivesName.set("offworld") }

dependencies {
    testImplementation(kotlin("test"))
}

tasks.test { useJUnitPlatform() }
