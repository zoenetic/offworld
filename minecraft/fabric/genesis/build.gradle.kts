plugins {
    id("org.jetbrains.kotlin.jvm")
    id("net.fabricmc.fabric-loom")
}

base { archivesName.set("offworld-genesis-fabric") }

repositories {
    mavenCentral()
    maven("https://maven.fabricmc.net")
}

val minecraftVersion = providers.gradleProperty("minecraft_version").get()
val loaderVersion = providers.gradleProperty("fabric_loader_version").get()
val fabricApiVersion = providers.gradleProperty("fabric_api_version").get()
val fabricLanguageKotlinVersion = providers.gradleProperty("fabric_language_kotlin_version").get()

dependencies {
    minecraft("com.mojang:minecraft:$minecraftVersion")
    implementation("net.fabricmc:fabric-loader:$loaderVersion")
    implementation("net.fabricmc.fabric-api:fabric-api:$fabricApiVersion")
    implementation("net.fabricmc:fabric-language-kotlin:$fabricLanguageKotlinVersion")
    implementation(project(":content"))
}

kotlin { jvmToolchain(25) }

loom {
    mods { register("offworld_genesis") { sourceSet(sourceSets.main.get()) } }
    runs {
        named("client") { vmArgs("--enable-native-access=ALL-UNNAMED") }
        named("server") { vmArgs("--enable-native-access=ALL-UNNAMED") }
    }
}

tasks.processResources {
    val props = mapOf("version" to project.version)
    inputs.properties(props)
    filesMatching("fabric.mod.json") { expand(props) }
}