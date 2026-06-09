//! genesis/ffi — the only crate with `unsafe` / C-ABI surface.
//!
//! A cdylib wrapping `genesis_core` behind a flat C interface (opaque handles +
//! a per-chunk buffer-fill entry point). Exists solely so the JVM can call
//! genesis via FFM/Panama — *not* JNI. `core` and `viz` never depend on this.
//!
//! The boundary trades data, not objects: a serializable `WorldSpec` in, a flat
//! `BlockId` buffer out. Crossed once per chunk, never per block.
//!
//! See docs/genesis-rust.md §3.2 and §5.
//!
//! TODO(you): `extern "C"` opaque handles + chunk-fill writing a shared buffer.
