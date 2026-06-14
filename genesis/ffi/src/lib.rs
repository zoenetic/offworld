use genesis_core::{
    ByMoisture, Constant, Draped, Environment, Field, FieldExt, Generator, GradientNoise, Layer, LayeredDeposition,
    MaterialId, Region, Snowy, ValueNoise, World, WorldBounds,
};

const BEDROCK: MaterialId = MaterialId(1);
const LIMESTONE: MaterialId = MaterialId(2);
const SHALE: MaterialId = MaterialId(3);
const SANDSTONE: MaterialId = MaterialId(4);
const STONE: MaterialId = MaterialId(5);
const SCREE: MaterialId = MaterialId(6);
const SOIL: MaterialId = MaterialId(7);
const SAND: MaterialId = MaterialId(8);
const SNOW: MaterialId = MaterialId(9);

fn uplift() -> impl Field {
    ValueNoise::new(30).frequency(0.003).octaves(2, 2.0, 0.5).scale(160.0)
}

fn offworld() -> World<LayeredDeposition> {
    let mut env = Environment::new();

    let bedrock_t = env.add(
        ValueNoise::new(11).frequency(0.006).octaves(4, 2.0, 0.5).scale(16.0).add(Constant(12.0)),
    );
    let stone_t = env.add(
        GradientNoise::new(15).frequency(0.012).octaves(4, 2.0, 0.5).scale(50.0)
            .add(uplift())
            .add(Constant(10.0)),
    );
    let soil_t = env.add(
        ValueNoise::new(17).frequency(0.03).octaves(3, 2.0, 0.5).scale(4.0).add(Constant(1.0)),
    );
    let landform = env.add(
        GradientNoise::new(15).frequency(0.012).octaves(3, 2.0, 0.5).scale(50.0)
            .add(uplift())
            .add(Constant(20.0)),
    );
    let tectonic = env.add(
        ValueNoise::new(12).frequency(0.006).octaves(2, 2.0, 0.5).scale(20.0).add(Constant(-10.0)),
    );
    let moisture = env.add(ValueNoise::new(40).frequency(0.004).octaves(3, 2.0, 0.5));

    let rule = LayeredDeposition {
        beds: vec![
            Layer::fixed(BEDROCK, bedrock_t),
            Layer::fixed(LIMESTONE, env.add(Constant(10.0))),
            Layer::fixed(SHALE, env.add(Constant(8.0))),
            Layer::fixed(SANDSTONE, env.add(Constant(10.0))),
            Layer::fixed(STONE, stone_t),
        ],
        mantle: Layer::selected(
            Snowy {
                snow: SNOW,
                below: Box::new(Draped {
                    over: SCREE,
                    under: Box::new(ByMoisture { wet: SOIL, dry: SAND, threshold: 0.4 }),
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

    World {
        environment: env,
        generator: Generator::new(rule),
        bounds: WorldBounds { min_y: 0.0, max_y: 320.0 },
    }
}

#[unsafe(no_mangle)]
pub extern "C" fn genesis_region_len(spacing: f64, nx: usize, nz: usize) -> usize {
    let world = offworld();
    let ny = (world.bounds.height() / spacing).ceil() as usize;
    nx * ny * nz
}

#[unsafe(no_mangle)]
pub unsafe extern "C" fn genesis_generate(
    min_x: f64,
    min_z: f64,
    spacing: f64,
    nx: usize,
    nz: usize,
    out_solidity: *mut f32,
    out_material: *mut u16,
    out_len: usize,
) -> i32 {
    let world = offworld();
    let fields = world.generate(&Region { min_x, min_z, spacing, nx, nz });
    let expected = fields.solidity.nx * fields.solidity.ny * fields.solidity.nz;
    if out_solidity.is_null() || out_material.is_null() || out_len != expected {
        return 1;
    }
    let sol = unsafe { std::slice::from_raw_parts_mut(out_solidity, out_len) };
    for (dst, &src) in sol.iter_mut().zip(fields.solidity.as_slice()) {
        *dst = src as f32;
    }
    let mat = unsafe { std::slice::from_raw_parts_mut(out_material, out_len) };
    for (dst, &src) in mat.iter_mut().zip(fields.material.as_slice()) {
        *dst = src.0;
    }

    0
}