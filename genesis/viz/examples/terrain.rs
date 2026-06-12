use genesis_core::{Constant, Draped, Environment, FieldExt, Generator, Layer, LayeredDeposition, Material, MaterialCatalogue, MaterialId, Region, ThermalErosion, ValueNoise, Vec3, World, WorldBounds};
use genesis_viz::{mesh_blocky, mesh_smooth, render_material_slice, render_vertical_slice, write_pgm, write_ply, write_ppm};

fn main() -> std::io::Result<()> {
    let mut catalogue = MaterialCatalogue::new();
    let bedrock = catalogue.add(Material { name: "bedrock".into(), hardness: 0.95, colour: [60, 60, 60] });
    let scree = catalogue.add(Material { name: "scree".into(), hardness: 0.30, colour: [150, 140, 120] });
    let sediment = catalogue.add(Material { name: "sediment".into(), hardness: 0.10, colour: [194, 178, 128] });
    let stone = catalogue.add(Material { name: "stone".into(), hardness: 0.70, colour: [130, 130, 130] });
    let soil = catalogue.add(Material { name: "soil".into(), hardness: 0.20, colour: [110, 80, 50] });

    let mut env = Environment::new();
    let bedrock_t = env.add(
        ValueNoise::new(10).frequency(0.006).octaves(4, 2.0, 0.5).scale(16.0).add(Constant(12.0))
    );
    let stone_t = env.add(
        ValueNoise::new(5).frequency(0.012).octaves(5, 2.0, 0.5).scale(50.0)
            .add(ValueNoise::new(8).frequency(0.05).octaves(2, 2.0, 0.5).scale(6.0))
            .max(ValueNoise::new(9).frequency(0.02).octaves(3, 2.0, 0.5).scale(40.0))
            .add(Constant(10.0))
    );
    let soil_t = env.add(
        ValueNoise::new(7).frequency(0.03).octaves(3, 2.0, 0.5).scale(4.0).add(Constant(1.0))
    );

    let landform = env.add(
        ValueNoise::new(5).frequency(0.012).octaves(3, 2.0, 0.5).scale(50.0).add(Constant(20.0))
    );

    let rule = LayeredDeposition {
        layers: vec![
            Layer::fixed(bedrock, bedrock_t),
            Layer::fixed(stone, stone_t),
            Layer::selected(Draped {
                over: scree,
                under: soil,
                max_depth: 4.0,
                gentle: 0.3,
                steep: 0.6,
            }, soil_t),
        ],
        landform,
    };

    let generator = Generator::new(rule)
        .with_erosion(ThermalErosion {
            iterations: 30,
            talus: 0.0,
            rate: 0.3,
            sediment,
        });

    let mut max_slope = 0.0f64;
    for z in (0..256).step_by(4) {
        for x in (0..256).step_by(4) {
            let (xf, zf) = (x as f64, z as f64);
            let e = 6.0;
            let h = |a: f64, b: f64| [bedrock_t, stone_t, soil_t].iter()
                .map(|&id| env.sample(id, Vec3::new(a, 0.0, b))).sum::<f64>();
            let gx = (h(xf + e, zf) - h(xf - e, zf)) / (2.0 * e);
            let gz = (h(xf, zf + e) - h(xf, zf - e)) / (2.0 * e);
            max_slope = max_slope.max((gx * gx + gz * gz).sqrt());
        }
    }
    eprintln!("max slope ≈ {max_slope:.3}");

    let world = World {
        environment: env,
        generator,
        bounds: WorldBounds { min_y: 0.0, max_y: 128.0 },
    };

    let fields = world.generate(&Region { min_x: 0.0, min_z: 0.0, spacing: 1.0, nx: 256, nz: 256 });

    write_pgm(&render_vertical_slice(&fields.solidity, 256, 128, 1.0, 0.0), "terrain.pgm")?;

    let palette = |m: MaterialId| {
        if m == MaterialId::NONE {
            [135, 206, 235]
        } else {
            catalogue.get(m).map_or([255, 0, 255], |mat| mat.colour) // truly unknown → magenta
        }
    };

    write_ppm(&render_material_slice(&fields.material, palette, 256, 128, 1.0, 0.0), "strata.ppm")?;

    let blocky_mesh = mesh_blocky(&fields.solidity, &fields.material, 0.5, palette);
    let smooth_mesh = mesh_smooth(&fields.solidity, &fields.material, 0.5, palette);

    write_ply(&blocky_mesh, "world_blocky.ply")?;
    write_ply(&smooth_mesh, "world_smooth.ply")?;

    Ok(())
}