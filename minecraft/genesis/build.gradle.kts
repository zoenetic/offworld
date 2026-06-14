plugins {
    id("org.jetbrains.kotlin.jvm")
    id("offworld.vanilla")
}

base { archivesName.set("offworld-genesis") }

repositories {
    mavenCentral()
    maven("https://maven.fabricmc.net")
}

val minecraftVersion = providers.gradleProperty("minecraft_version").get()

dependencies {
    testImplementation(kotlin("test"))
}

kotlin { jvmToolchain(25) }

// GenesisLibrary (dev.offworld.content) binds the genesis-ffi Rust crate via FFI and
// loads libgenesis.so from the classpath, so the native build lives alongside it here.
val nativeTarget = "linux-x86-64"
val repoRoot = rootDir.parentFile

val cargoBuild by tasks.registering(Exec::class) {
    workingDir = repoRoot
    commandLine("cargo", "build", "-p", "genesis-ffi")
}

val copyNative by tasks.registering(Copy::class) {
    dependsOn(cargoBuild)
    from(repoRoot.resolve("target/debug/libgenesis.so"))
    into(layout.buildDirectory.dir("generated/resources/native/$nativeTarget"))
}

sourceSets.main {
    resources.srcDir(layout.buildDirectory.dir("generated/resources/"))
}

tasks.named("processResources") { dependsOn(copyNative) }

tasks.test {
    useJUnitPlatform()
    jvmArgs("--enable-native-access=ALL-UNNAMED")
}
