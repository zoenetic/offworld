use genesis_core::{ByMoisture, Constant, Draped, Environment, Field, FieldExt, Generator, GradientNoise, Layer, LayeredDeposition, Material, MaterialCatalogue, MaterialId, Region, Snowy, ThermalErosion, ValueNoise, Vec3, World, WorldBounds};
use genesis_viz::{mesh_blocky, mesh_smooth, render_material_slice, render_vertical_slice, write_pgm, write_ply, write_ppm};

const SIZE: usize = 1024;
const HEIGHT: usize = 384;
const SPACING: f64 = 1.0;

fn uplift() -> impl Field {
    ValueNoise::new(30).frequency(0.003).octaves(2, 2.0, 0.5).scale(160.0)
}

fn main() -> std::io::Result<()> {
    let mut catalogue = MaterialCatalogue::new();
    let bedrock = catalogue.add(Material { name: "bedrock".into(), hardness: 0.95, colour: [60, 60, 60] });
    let limestone = catalogue.add(Material { name: "limestone".into(), hardness: 0.95, colour: [70, 70, 70] });
    let sand = catalogue.add(Material { name: "sand".into(), hardness: 0.10, colour: [210, 190, 140] });
    let sandstone = catalogue.add(Material { name: "sandstone".into(), hardness: 0.95, colour: [90, 90, 90] });
    let shale = catalogue.add(Material { name: "shale".into(), hardness: 0.95, colour: [80, 80, 80] });
    let scree = catalogue.add(Material { name: "scree".into(), hardness: 0.30, colour: [150, 140, 120] });
    let sediment = catalogue.add(Material { name: "sediment".into(), hardness: 0.10, colour: [194, 178, 128] });
    let snow = catalogue.add(Material { name: "snow".into(), hardness: 0.10, colour: [235, 240, 250] });
    let stone = catalogue.add(Material { name: "stone".into(), hardness: 0.70, colour: [130, 130, 130] });
    let soil = catalogue.add(Material { name: "soil".into(), hardness: 0.20, colour: [110, 80, 50] });

    let mut env = Environment::new();
    let bedrock_t = env.add(
        ValueNoise::new(11).frequency(0.006).octaves(4, 2.0, 0.5).scale(16.0).add(Constant(12.0))
    );
    let stone_t = env.add(
        ValueNoise::new(15).frequency(0.012).octaves(5, 2.0, 0.5).scale(50.0)
            .add(ValueNoise::new(8).frequency(0.05).octaves(2, 2.0, 0.5).scale(6.0))
            .max(ValueNoise::new(9).frequency(0.02).octaves(3, 2.0, 0.5).scale(40.0))
            .add(uplift())
            .add(Constant(10.0))
    );
    let soil_t = env.add(
        ValueNoise::new(17)
            .frequency(0.03)
            .octaves(3, 2.0, 0.5)
            .scale(4.0)
            .add(Constant(1.0))
    );

    let landform = env.add(
        GradientNoise::new(15)
            .frequency(0.012)
            .octaves(3, 2.0, 0.5)
            .scale(50.0)
            .add(uplift())
            .add(Constant(20.0))
    );

    let tectonic = env.add(
        ValueNoise::new(12)
            .frequency(0.006)
            .octaves(2, 2.0, 0.5)
            .add(Constant(-10.0))
    );

    let moisture = env.add(ValueNoise::new(40).frequency(0.004).octaves(3, 2.0, 0.5));

    let rule = LayeredDeposition {
        beds: vec![
            Layer::fixed(bedrock, bedrock_t),
            Layer::fixed(limestone, env.add(Constant(10.0))),
            Layer::fixed(shale, env.add(Constant(8.0))),
            Layer::fixed(sandstone, env.add(Constant(10.0))),
            Layer::fixed(stone, stone_t),
        ],
        mantle: Layer::selected(
            Snowy {
                snow,
                below: Box::new(Draped {
                    over: scree,
                    under: Box::new(ByMoisture { wet: soil, dry: sand, threshold: 0.4 }),
                    max_depth: 4.0,
                    gentle: 0.3,
                    steep: 0.6,
                }),
                freezing: 0.0,
            },
            soil_t,
        ),
        landform,
        tectonic,
        moisture,
        sea_level_temp: 20.0,
        lapse_rate: 0.125,
    };

    let generator = Generator::new(rule)
        .with_erosion(ThermalErosion {
            iterations: 30,
            talus: 0.0,
            rate: 0.3,
            sediment,
        });

    println!("Calculating max_slope...");

    let mut max_slope = 0.0f64;
    for z in (0..SIZE).step_by(4) {
        for x in (0..SIZE).step_by(4) {
            let (xf, zf) = (x as f64 * SPACING, z as f64 * SPACING);
            let e = 6.0;
            let h = |a: f64, b: f64| [bedrock_t, stone_t, soil_t].iter()
                .map(|&id| env.sample(id, Vec3::new(a, 0.0, b))).sum::<f64>();
            let gx = (h(xf + e, zf) - h(xf - e, zf)) / (2.0 * e);
            let gz = (h(xf, zf + e) - h(xf, zf - e)) / (2.0 * e);
            max_slope = max_slope.max((gx * gx + gz * gz).sqrt());
        }
    }

    let world = World {
        environment: env,
        generator,
        bounds: WorldBounds { min_y: 0.0, max_y: HEIGHT as f64 * SPACING },
    };

    println!("Generating world...");

    let fields = world.generate(&Region { min_x: 0.0, min_z: 0.0, spacing: SPACING, nx: SIZE, nz: SIZE });

    println!("Writing solidity slice...");

    write_pgm(&render_vertical_slice(&fields.solidity, SIZE, HEIGHT, SPACING, 0.0), "solidity.pgm")?;

    let palette = |m: MaterialId| {
        if m == MaterialId::NONE {
            [135, 206, 235]
        } else {
            catalogue.get(m).map_or([255, 0, 255], |mat| mat.colour) // truly unknown → magenta
        }
    };

    println!("Writing strata slice...");

    write_ppm(&render_material_slice(&fields.material, palette, SIZE, HEIGHT, SPACING, 0.0), "strata.ppm")?;

    println!("Generating blocky mesh...");

    let blocky_mesh = mesh_blocky(&fields.solidity, &fields.material, 0.5, palette);

    println!("Writing blocky mesh...");

    write_ply(&blocky_mesh, "world_blocky.ply")?;

    println!("Generating smooth mesh...");

    let smooth_mesh = mesh_smooth(&fields.solidity, &fields.material, 0.5, palette);

    println!("Writing smooth mesh...");

    write_ply(&smooth_mesh, "world_smooth.ply")?;

    Ok(())
}