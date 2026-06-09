use genesis_core::{FieldExt, ValueNoise};
use genesis_viz::{render_field_slice, write_pgm};

fn main() -> std::io::Result<()> {
    let field = ValueNoise::new(1).frequency(0.015);
    let img = render_field_slice(&field, 512, 512, 1.0);
    write_pgm(&img, "noise.pgm")?;
    println!("wrote noise.pgm ({}x{})", img.width, img.height);
    Ok(())
}