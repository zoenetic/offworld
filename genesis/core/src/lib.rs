//! genesis/core — the pure, host-agnostic field-up worldgen engine.
//!
//! Pure Rust. Knows nothing about hosts, the JVM, or `genesis/ffi`. Thinks only
//! in integers and coordinates (`BlockId`, `BiomeId`, `Vec3i`) and paints onto a
//! `Canvas`. Generation is a pure function of (seed, `WorldSpec`, position).
//!
//! See docs/genesis-rust.md §3 (workspace discipline) and §6 (the names).
//!
//! TODO(you): the engine lives here — World/Canvas/Environment/Generator.
