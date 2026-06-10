plugins {
    kotlin("jvm") version "2.4.0"
}

repositories { mavenCentral() }

kotlin { jvmToolchain(25) }

base { archivesName.set("offworld-content") }

dependencies {
    testImplementation(kotlin("test"))
}

val nativeTarget = "linux-x86-64"

val cargoBuild by tasks.registering(Exec::class) {
    description = "cargo build"
    workingDir = rootDir
    commandLine("cargo", "build", "-p", "genesis-ffi")
}

val copyNative by tasks.registering(Copy::class) {
    description = "copy native build"
    dependsOn(cargoBuild)
    from(rootDir.resolve("target/debug/libgenesis.so"))
    into(layout.buildDirectory.dir("generated/resources/native/$nativeTarget"))
}

sourceSets.main {
    resources.srcDir(layout.buildDirectory.dir("generated/resources"))
}

tasks.named("processResources") { dependsOn(copyNative) }

tasks.test {
    useJUnitPlatform()
    jvmArgs("--enable-native-access=ALL-UNNAMED")
}