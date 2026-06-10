use genesis_core::{Accrete, Environment, FieldExt, Generator, Region, ValueNoise, World, WorldBounds};

fn demo_world() -> World {
    let mut env = Environment::new();
    let thickness = env.add(ValueNoise::new(1).frequency(0.02).scale(64.0));
    World {
        environment: env,
        generator: Generator::new(Accrete { thickness }),
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
pub unsafe extern "C" fn genesis_generate_solidity(
    min_x: f64,
    min_z: f64,
    spacing: f64,
    nx: usize,
    nz: usize,
    out: *mut f32,
    out_len: usize,
) -> i32 {
    let world = demo_world();
    let grid = world.generate(&Region { min_x, min_z, spacing, nx, nz});

    if out.is_null() || out_len != grid.nx * grid.ny * grid.nz {
        return 1;
    }

    let buf = unsafe { std::slice::from_raw_parts_mut(out, out_len) };
    for (dst, &src) in buf.iter_mut().zip(grid.as_slice()) {
        *dst = src as f32;
    }
    0
}