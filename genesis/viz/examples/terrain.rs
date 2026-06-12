use genesis_core::{deposit_region, Accrete, Environment, FieldExt, Generator, MaterialId, Region, ValueNoise, Vec3, World, WorldBounds, Strata};
use genesis_viz::{mesh_blocky, mesh_smooth, render_material_slice, render_vertical_slice, write_pgm, write_ply, write_ppm};

fn main() -> std::io::Result<()> {
    let mut env = Environment::new();

    let hills = ValueNoise::new(3).frequency(0.0125).octaves(5, 2.0, 0.5);
    let ridges = ValueNoise::new(4).frequency(0.025).octaves(4, 2.0, 0.5);

    let thickness = env.add(hills.max(ridges).scale(100.0));

    let rule = Strata {
        thickness,
        bedrock: MaterialId(1),
        stone: MaterialId(2),
        soil: MaterialId(3),
        bedrock_depth: 4.0,
        soil_depth: 5.0,
    };

    let world = World {
        environment: env,
        generator: Generator::new(rule),
        bounds: WorldBounds { min_y: 0.0, max_y: 128.0 }
    };

    let fields = world.generate(&Region { min_x: 0.0, min_z: 0.0, spacing: 1.0, nx: 256, nz: 256});

    write_pgm(&render_vertical_slice(&fields.solidity, 256, 128, 1.0, 0.0), "terrain.pgm")?;

    let palette = |m: MaterialId| match m.0 {
        0 => [135, 206, 235],
        1 => [ 60,  60,  60],
        2 => [130, 130, 130],
        3 => [110,  80,  50],
        _ => [255,   0, 255],
    };

    write_ppm(&render_material_slice(&fields.material, palette, 256, 128, 1.0, 0.0), "strata.ppm")?;

    let blocky_mesh = mesh_blocky(&fields.solidity, &fields.material, 0.5, palette);
    let smooth_mesh = mesh_smooth(&fields.solidity, &fields.material, 0.5, palette);

    write_ply(&blocky_mesh, "world_blocky.ply")?;
    write_ply(&smooth_mesh, "world_smooth.ply")?;

    Ok(())
}