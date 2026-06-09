rootProject.name = "offworld"

// ---------------------------------------------------------------------------
// Gradle owns minecraft/** only. The Rust half (genesis/*, game) belongs to
// Cargo and is intentionally invisible here — no `include` ever points at it.
//
// The Gradle path tree starts at :offworld:* but lives on disk under
// minecraft/offworld/* (a disk-only grouping). We rebase each include below so
// e.g. :offworld:fabric:tech resolves to minecraft/offworld/fabric/tech.
// See docs/genesis-rust.md §4.
// ---------------------------------------------------------------------------

// Common modules + the four published per-loader leaves.
val offworldModules = listOf(
    "content",        // WorldSpec + block/biome registration; bundles the cdylib
    "menu",           // common start-screen logic (optional)
    "tech",           // common gameplay/worldgen logic (optional)
    "fabric:menu",    // -> archivesName offworld-menu-fabric
    "fabric:tech",    // -> archivesName offworld-tech-fabric
    "neoforge:menu",  // -> archivesName offworld-menu-neoforge
    "neoforge:tech",  // -> archivesName offworld-tech-neoforge
)

// Rebase the logical :offworld container itself, then each module, onto disk.
include(":offworld")
project(":offworld").projectDir = file("minecraft/offworld")

for (m in offworldModules) {
    val path = ":offworld:$m"
    include(path)
    // ":offworld:fabric:tech" -> "minecraft/offworld/fabric/tech"
    project(path).projectDir = file("minecraft/" + path.removePrefix(":").replace(':', '/'))
}
