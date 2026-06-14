use genesis_core::{
    Constant, Environment, FieldExt, Generator, Layer, LayeredDeposition,
    MaterialId, Region, ValueNoise, World, WorldBounds,
};

fn demo_world() -> World<LayeredDeposition> {
    let mut env = Environment::new();

    let bedrock_t = env.add(Constant(8.0));
    let stone_t = env.add(
        ValueNoise::new(1).frequency(0.02).octaves(4, 2.0, 0.5).scale(60.0).add(Constant(10.0)),
    );
    let soil_t = env.add(Constant(4.0));
    let landform = env.add(Constant(0.0));
    let tectonic = env.add(Constant(0.0));

    let rule = LayeredDeposition {
        beds: vec![
            Layer::fixed(MaterialId(1), bedrock_t),
            Layer::fixed(MaterialId(2), stone_t),
        ],
        mantle: Layer::fixed(MaterialId(3), soil_t),
        landform,
        tectonic,
        sea_level_temp: 20.0,
        lapse_rate: 0.2,
        moisture: env.add(Constant(0.0)),
    };

    World {
        environment: env,
        generator: Generator::new(rule),
        bounds: WorldBounds { min_y: 0.0, max_y: 128.0 },
    }
}

#[unsafe(no_mangle)]
pub extern "C" fn genesis_region_len(spacing: f64, nx: usize, nz: usize) -> usize {
    let world = demo_world();
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
    let world = demo_world();
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