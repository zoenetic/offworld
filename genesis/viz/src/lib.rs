//! genesis/viz — the headless render core (the tuning instrument).
//!
//! Draws genesis's internal state for inspection and regression:
//! `render_field_slice`, `render_biome_map`, `render_column_slice`, … plus a
//! golden-image harness. Pure: depends on `genesis_core` only.
//!
//! Minecraft is the *acceptance test*; this is the *instrument*. The loop closes
//! through the serializable `WorldSpec`. See docs/genesis-rust.md §7.
//!
//! TODO(you): render core (Tier 1) + PNG-dump / golden images (Tier 2).
