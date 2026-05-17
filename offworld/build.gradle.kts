plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.fabric.loom)
    `java-test-fixtures`
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(25)
}

kotlin {
    jvmToolchain(25)
}

dependencies {
    minecraft("com.mojang:minecraft:${libs.versions.minecraft.get()}")

    testImplementation(libs.kotlin.test)
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = false
    }
}
