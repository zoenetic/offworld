// offworld :: content — the JVM adapter ring.
// Owns BlockPalette (BlockId <-> real block), MinecraftCanvas (drains genesis's
// per-chunk buffer), GenesisChunkGenerator/GenesisBiomeSource. Bundles + loads
// the genesis cdylib via FFM/Panama. This is the only JVM module that touches
// the native library. See docs/genesis-rust.md §2, §5.
plugins { base }

base { archivesName.set("offworld-content") }

// TODO(you): java/kotlin + the FFM bindings; bundle libgenesis from genesis/ffi.
