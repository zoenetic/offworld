plugins {
    alias(libs.plugins.fabric.loom)
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(25)
    withSourcesJar()
}

kotlin {
    jvmToolchain(25)
}

base {
    archivesName = "offworld"
}

loom {
    splitEnvironmentSourceSets()

    mods {
        create("offworld") {
            sourceSet("main")
            sourceSet("client")
        }
    }
}

dependencies {
    minecraft("com.mojang:minecraft:${libs.versions.minecraft.get()}")

    implementation(libs.fabric.loader)
    implementation(libs.fabric.api)
    implementation(libs.fabric.language.kotlin)

    implementation(project(":offworld"))
    implementation(testFixtures(project(":offworld")))
    include(project(":offworld"))
}

tasks.processResources {
    val versionValue = project.version.toString()
    inputs.property("version", versionValue)

    filesMatching("fabric.mod.json") {
        expand("version" to versionValue)
    }
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = false
    }
}

val texturegen by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
}

dependencies {
    texturegen(project(":offworld"))
}

val generatePlaceholderAssets by tasks.registering(JavaExec::class) {
    val outDir = layout.buildDirectory.dir("generated/assets/main")

    classpath = texturegen
    mainClass.set("dev.zoenetic.offworld.tools.TexturegenMainKt")
    args(outDir.get().asFile.absolutePath)

    inputs.files(texturegen)
    outputs.dir(outDir)
}