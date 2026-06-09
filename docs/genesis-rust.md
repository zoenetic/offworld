# Genesis — a field-up worldgen engine, built in Rust

Status: **plan of record** (genesis not started yet; the existing engine keeps
running). This supersedes the *substrate* decision in `genesis-refactor.md`:
genesis is built in **Rust**, not Kotlin Multiplatform. The field-up model, the
three-rings architecture, and the genesis vocabulary carry over unchanged — see
§9 for exactly what is kept vs. replaced.

## What this document is

A plan to build **genesis**: a self-contained, engine-agnostic worldgen engine,
designed natively around the *field-up* generation model, written in **Rust**.
Genesis is built **in parallel** with the current Kotlin engine — the old code
keeps generating worlds the whole time, and we port pieces across, reimagining
them in the new model, until genesis reaches parity and we switch over.

Genesis serves **two mandates at once**:

1. **Now:** power the Minecraft mod. The JVM consumes genesis through a thin
   **C-ABI cdylib** (`genesis/ffi`) via **FFM/Panama** — *not* JNI. The boundary
   is crossed **once per chunk**, never per block.
2. **Now-ish, and for real:** a native standalone game (`game/`) that consumes
   `genesis/core` **directly, with no FFI at all**.

The whole point of the structure below is that the engine (`genesis/core`) names
nothing platform-specific, the FFI layer exists *only* for the JVM host, and the
native game pays no boundary tax whatsoever.

Read it top to bottom: §1 the fork and why; §2 the rings; §3 the Rust workspace
discipline; §4 the target structure; §5 build & boundary rules; §6 the names;
§7 the field-up flow; §8 the build-and-port plan; §9 relationship to the KMP plan.

---

## 1. The fork: why Rust (not KMP)

`genesis-refactor.md` chose Kotlin Multiplatform **specifically to avoid an FFI
boundary** — the JVM mod would reuse the engine as a plain Kotlin library, zero
binding layer. That plan is now superseded. Rust reintroduces a boundary *on the
JVM side*, and we accept that cost deliberately because:

- **A true native standalone.** `game/` links `genesis/core` directly as a Rust
  crate — no FFI, no JVM, no marshalling. KMP's "native target" would have meant
  Kotlin/Native, whose perf on sim-heavy paths is unproven; Rust is the native
  target.
- **Raw simulation throughput.** Field-up is "cellular-automaton-with-many-
  physical-dimensions"; the engine is the hot path. Rust gives predictable,
  allocation-free numerics and SIMD without a managed runtime in the loop.
- **The boundary is bounded and one-directional.** We pay the FFM tax exactly
  once per chunk (§5), and *only* the JVM hosts pay it. `core` never knows `ffi`
  exists; `game` never touches it.

The trade we are accepting: a dual **Cargo + Gradle** build, a cdylib to package
and distribute per loader, and two toolchains in CI — in exchange for a native
standalone with no binding layer and native sim speed. This is the inverse of the
KMP bet, made knowingly.

---

## 2. The mental model: three rings (unchanged)

```
  HOST (Fabric/NeoForge mod  |  native game)   ← platform lifecycle
    └ ADAPTER (palette + canvas; the cdylib drain on the JVM side)
        └ GENESIS = genesis/core (the engine)  ← pure Rust; knows no host
```

- **Genesis** (`genesis/core`) is pure Rust. It thinks only in integers and
  coordinates — `BlockId(5)`, `Vec3i { x, y, z }` — and paints onto a **Canvas**.
  It never names a Minecraft type, a JVM type, or the FFI layer.
- The **Adapter** is the one bilingual layer. On the JVM that is `content`:
  it owns the **BlockPalette** (`BlockId ↔ real block`, `BiomeId ↔ real biome`),
  bundles + loads the cdylib, and implements **Canvas** by draining genesis's
  per-chunk buffer into the host's "set block." In the native game the adapter is
  trivial — `game` calls `core` directly.
- The **Host** owns the block registry, chunk storage, and the lifecycle hook
  that asks "generate this chunk." Today on the JVM that's a per-loader mod
  (`fabric`/`neoforge`); natively it's `game`.

Dependencies point **inward only**: `host → adapter → genesis`. The engine points
at nothing platform-specific. A new platform = a new outer two rings; `core` is
reused verbatim.

---

## 3. The Rust workspace discipline

This is what "write it so the boundary stays clean" means concretely.

1. **`genesis/core` is `#![no_std]`-friendly, pure, and FFI-blind.** No JVM types,
   no `genesis/ffi` dependency, no host concepts. It deals in `BlockId`,
   `BiomeId`, `Vec3i`, `Field`, `Canvas`. `core` does not know it will ever be
   called across a C ABI.
2. **`genesis/ffi` is the *only* crate with `unsafe`/C-ABI surface.** It is a
   `cdylib` that wraps `core` behind a flat C interface (opaque handles + a
   chunk-fill entry point writing a shared `BlockId` buffer). It exists solely so
   the JVM can call genesis. Nothing in `core` or `viz` depends on it.
3. **Determinism by construction.** Generation is a pure function of (seed,
   `WorldSpec`, position). Seeded positional RNG, deliberate floating-point — so
   the *same world* appears in the mod and in the native game.
4. **The FFI contract is data, not objects.** The boundary trades a `WorldSpec`
   (serializable) in and a flat `BlockId` buffer out. No object graphs cross; no
   callbacks per block. This keeps the marshalling cost at §5's "once per chunk."
5. **`viz` is pure too.** The headless render core depends on `core` only; PNG
   encoding is a `viz` concern, never a `core` one.

If these hold, the native game and the JVM mod share one engine, and the only
crate that is ever "dangerous" is the small `ffi` shim.

---

## 4. Target structure

```
offworld/                    repo root; Cargo.toml + settings.gradle.kts side by side
├─ game/                     Rust standalone host → genesis/core (no FFI)
├─ genesis/                  ── Rust engine (Cargo workspace) ──
│  ├─ core/                  pure engine; no FFI, no JVM types
│  ├─ ffi/                   cdylib / C ABI; exists ONLY for the JVM side
│  └─ viz/                   headless render core (field slices, golden images)
└─ minecraft/                ── JVM / Gradle; disk-only grouping ──
   └─ offworld/              Gradle path starts here → :offworld:*  (dir remapped via helper)
      ├─ content/            WorldSpec + block/biome registration; bundles + loads the cdylib
      ├─ menu/               common start-screen logic (optional)
      ├─ tech/               common gameplay logic (optional)
      ├─ fabric/{menu,tech}  thin per-loader mods → :offworld:menu / :offworld:tech (+content)
      └─ neoforge/{menu,tech} thin per-loader mods
```

- **Cargo** owns `genesis/*` + `game`. **Gradle** owns `minecraft/**`. Two
  manifests sit at the root and **ignore each other** by explicit membership on
  both sides.
- The Gradle path `:offworld:…` deliberately diverges from disk
  `minecraft/offworld/…`; a rebase helper in `settings.gradle.kts` remaps it.

---

## 5. Build & boundary rules

- **FFI is opt-in per host.** `game` → `core` natively. `content`/`tech` → the
  `ffi` cdylib via **FFM/Panama** (not JNI). `core` never knows `ffi` exists.
- **Cross once per chunk, never per block.** Rust fills a flat shared buffer of
  `BlockId`s for the whole chunk; the JVM `MinecraftCanvas` drains it in a single
  pass into the real `ChunkAccess`. This is the entire perf argument for the
  boundary being acceptable.
- **The cdylib ships only with `tech`/`content`** — never with the cosmetic
  `menu` mod, which touches neither genesis nor the native library.
- **Four published leaf jars need distinct `archivesName`:**
  `offworld-{menu,tech}-{fabric,neoforge}`.
- **Two toolchains, two CIs.** Cargo builds `genesis/*` + `game` and emits the
  cdylib per target platform; Gradle builds the loader jars and bundles the
  matching cdylib into `content`/`tech`.

---

## 6. Names (the genesis vocabulary — carried over)

Same scheme as the KMP plan; now Rust types. Anchors: **World, Canvas,
Environment, Generator.**

- **The world:** `WorldSpec` (declarative, serializable description) →
  `WorldBuilder` (compiles spec + seed) → `World` (assembled, seeded, ready) →
  `WorldBounds` (vertical extent + seed).
- **The continuous layer:** `Field` (`sample(x,y,z) -> f64`), `FieldId`,
  `Environment` (registry of named fields), `EnvironmentSample`,
  `FieldSampler`/`FieldGrid`, combinators (`Add`/`Mul`/`Scale`/`Clamp`/`Spline`),
  `WindField`/`WindSample`.
- **The discrete layer:** `Generator` (per-chunk driver) running
  `DepositionRule` + `ColumnState` (transport-capable) → `Erosion`
  (`NoErosion` default) → `Seeder` × `GrowthRule` → `Carver`, all writing
  `Canvas`.
- **Biome is a label, not a stage:** `BiomeLabeller` (pure
  `EnvironmentSample → BiomeBlend` projection; also groups deposition/growth
  rules), `NearestBiomeLabeller`, `BiomeBlend`.
- **Identifiers & adapter:** `BlockId`/`BiomeId` (opaque ints), `Vec3i`,
  `Catalog` (human keys → ids while building); on the JVM side `BlockPalette`,
  `MinecraftCanvas`, `GenesisChunkGenerator`/`GenesisBiomeSource`.

---

## 7. How genesis generates a chunk (the field-up flow — unchanged)

The **Environment** is the hub. **Biome is not a stage** — it's an ambient label,
a pure projection `EnvironmentSample → BiomeBlend`, queried wherever a name is
needed (the host's biome source, debug display, rule grouping). Because the label
is a function of the *fields*, it's available before generation and keeps
generation chunk-independent.

Generation is four stages, each reading the Environment and writing the Canvas:

1. **Deposition (bottom-up)** — sweep the column from `minY` up carrying a
   `ColumnState`; at each height sample the Environment and apply the
   `DepositionRule`s grouped under the biome label holding here (bedrock → strata
   → surface → scree accretion, all from one mechanism), updating channels.
2. **Erosion** — `Erosion` reworks deposited material via `ColumnState`
   transport. **No-op by default**; runs here so growth sees the eroded surface.
3. **Growth (seeded)** — a `Seeder` turns a density field into seed positions;
   each seed runs a `GrowthRule` painting a structure (coral branches, wind =
   drift). Plain scatter is the degenerate case.
4. **Carve** — `Carver`s remove blocks for caves.

Every placement writes the per-chunk buffer. On the JVM the cdylib hands that one
buffer across the boundary and `MinecraftCanvas` drains it (§5). In `game` the
host reads the buffer directly.

**Tooling.** Minecraft is the *acceptance test*, not the tuning instrument. The
instrument is `genesis/viz`: a headless render core (`render_field_slice`,
`render_biome_map`, `render_column_slice`, …) that draws the model's internal
state, with a golden-image harness as the regression net. An interactive viewer
(native, off `viz`) is the later tier. The loop closes through the serializable
`WorldSpec`: the viewer tunes a spec, saves it, and the mod/game loads it — the
tool's output *is* the engine's input.

---

## 8. The build-and-port plan

The old Kotlin/Fabric engine is both the **reference** (port behaviour from it)
and the **fallback** (keeps the game working). Each phase ends green and renders
something you can look at headlessly.

- **Phase 0 — Stand up the workspace.** Root `Cargo.toml` workspace with
  `genesis/core`, `genesis/ffi`, `genesis/viz`, `game`. Declare the core
  types/traits in `core`; stand up `viz` Tier 1 (render core) + Tier 2 (PNG-dump
  / golden images). Exit: `cargo test` builds and renders an empty world; `core`
  has no `ffi`/JVM dependency.
- **Phase 1 — The field layer.** Port noise primitives + combinators; build
  `Environment`/`FieldId`/`EnvironmentSample` + `FieldSampler`/`FieldGrid`.
  Render a noise field headlessly.
- **Phase 2 — Deposition.** `DepositionRule` + `ColumnState` (transport) + the
  bottom-up sweep in `Generator`. Reimplement base terrain, strata, scree
  accretion. Render vertical slices.
- **Phase 2.5 — Erosion slot (no-op).** Add `Erosion` between deposition and
  growth, `NoErosion` default; register an `erosion` `FieldId`.
- **Phase 3 — Biome labels + extra axes.** `BiomeLabeller` as a pure projection
  (not a stage); register `heat`/`pressure`/`saturation` to prove axes vanilla
  can't carry.
- **Phase 4 — Growth.** `Seeder` + `GrowthRule`; reimplement `GlassGrass`
  (Seeder + single block) and `PaleCoralglass` (seeded branching; wind = drift).
- **Phase 5 — The FFI + JVM adapter.** Write `genesis/ffi` (cdylib, C ABI,
  per-chunk buffer fill). On the Gradle side write `content`: `BlockPalette`,
  `MinecraftCanvas` (drains the buffer), `GenesisChunkGenerator`/
  `GenesisBiomeSource`, and cdylib bundling/loading via FFM. Register as a
  **second dimension** (or behind a flag) so the existing `offworld` dimension is
  untouched and both coexist for comparison.
- **Phase 5b — The native host.** `game` links `core` directly and renders the
  same world with no FFI — the proof that the engine is host-agnostic.
- **Phase 6 — Parity and switchover.** Iterate until genesis matches/exceeds the
  old world's feel; point `offworld` at genesis; delete the old Kotlin pipeline,
  features, and dead glue.
- **Phase 6.5 — (deferred) real erosion.** Thermal/talus first (bounded,
  chunk-friendly, uses `ColumnState` transport); hydraulic later as a coarse
  global heightmap pre-pass to preserve chunk-independence.

---

## 9. Relationship to `genesis-refactor.md` and `worldgen-toolkit.md`

- **Kept verbatim from `genesis-refactor.md`:** the field-up *model*, the
  three-rings architecture, the genesis *vocabulary* (§6), the chunk *flow* (§7),
  the tooling philosophy (viz Tiers 1–3, golden images, `WorldSpec` closing the
  loop), and the phased *port* approach.
- **Superseded by this document:** the **substrate** (Rust, not KMP) and
  everything mechanical that follows from it — the §3 *portability discipline*
  (now a Rust workspace discipline, not `commonMain`/`expect`/`actual`), the §6
  *module layout* (now §4 here: Cargo + Gradle, `core`/`ffi`/`viz`/`game` +
  multi-loader `minecraft/**`), and the *zero-binding-layer* premise (the JVM now
  pays a bounded FFM boundary; only the native game is binding-free).
- `worldgen-toolkit.md` remains the earlier thinking that led to the field-up
  model (placement × structure → `Seeder` × `GrowthRule`; field-CA →
  `Environment` + `DepositionRule`). Still superseded; kept for lineage.

Treat **this document as the plan of record.** `genesis-refactor.md` is retained
for the model/vocabulary/flow it still defines, but where the two disagree on
language, build, or boundary, this one wins.
