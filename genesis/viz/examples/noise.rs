use genesis_core::{bake, FieldExt, ValueNoise, Vec3};
use genesis_viz::{render_field_slice, write_pgm};

fn main() -> std::io::Result<()> {
    let field = ValueNoise::new(1).frequency(0.015);
    let lazy = render_field_slice(&field, 512, 512, 1.0);
    write_pgm(&lazy, "noise.pgm")?;

    let grid = bake(&field, Vec3::new(0.0, 0.0, 0.0), 16.0, 32, 1, 32);
    let baked = render_field_slice(&grid, 512, 512, 1.0);
    write_pgm(&baked, "noise_baked.pgm")?;

    println!("wrote noise.pgm and noise_baked.pgm");
    Ok(())
}