// Root build for the Gradle half (minecraft/**). The Rust half (genesis/*, game)
// is owned by Cargo and invisible to Gradle — see settings.gradle.kts.
//
// Each module currently applies only the `base` plugin so the tree configures
// and `gradle projects` works. TODO(you): real loader plugins (Loom / NeoForge /
// Architectury), the Java 25 toolchain, and per-loader run configs.

subprojects {
    group = providers.gradleProperty("mod_group").get()
    version = providers.gradleProperty("mod_version").get()
}
