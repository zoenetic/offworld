use genesis_core::{deposit_region, Accrete, Environment, FieldExt, Generator, Region, ValueNoise, Vec3, World, WorldBounds};
use genesis_viz::{render_vertical_slice, write_pgm};

fn main() -> std::io::Result<()> {
    let mut env = Environment::new();
    let thickness = env.add(ValueNoise::new(1).frequency(0.02).scale(64.0));
    let rule = Accrete { thickness };

    let world = World {
        environment: env,
        generator: Generator::new(Accrete { thickness }),
        bounds: WorldBounds { min_y: 0.0, max_y: 128.0 }
    };

    let grid = world.generate(&Region { min_x: 0.0, min_z: 0.0, spacing: 1.0, nx: 256, nz: 1});

    let img = render_vertical_slice(&grid, 256, 128, 1.0, 0.0);
    write_pgm(&img, "terrain.pgm")?;
    println!("wrote terrain.pgm");
    Ok(())
}