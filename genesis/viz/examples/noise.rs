use genesis_core::{deposit_region, Accrete, Environment, FieldExt, ValueNoise, Vec3};
use genesis_viz::{render_vertical_slice, write_pgm};

fn main() -> std::io::Result<()> {
    let mut env = Environment::new();
    let thickness = env.add(ValueNoise::new(1).frequency(0.02).scale(64.0));
    let rule = Accrete { thickness };

    let grid = deposit_region(&env, &rule, Vec3::new(0.0, 0.0, 0.0), 1.0, 256, 128, 1);
    let img = render_vertical_slice(&grid, 256, 128, 1.0, 0.0);
    write_pgm(&img, "terrain.pgm")?;
    println!("wrote terrain.pgm");
    Ok(())
}