package world

import "core:math"
import "core:math/noise"

Terrain_Params :: struct {
	seed:        i64,
	base_height: f32,
	amplitude:   f32,
	frequency:   f32,
	octaves:     int,
}

DEFAULT_TERRAIN :: Terrain_Params {
	seed        = 1,
	base_height = 12,
	amplitude   = 6,
	frequency   = 0.03,
	octaves     = 4,
}

terrain_height :: proc(params: Terrain_Params, x, z: f32) -> f32 {
	value: f32 = 0
	for i in 1 ..= params.octaves {
		frequency := params.frequency * f32(i)
		amplitude := params.amplitude / f32(i)
		seed := params.seed + i64(i)
		value += amplitude * noise.noise_2d(seed, {f64(x), f64(z)} * f64(frequency))
	}
	return value + params.base_height
}

fill_terrain :: proc(chunk: ^Chunk, params: Terrain_Params) {
	o := origin(chunk.coord)
	for z in -APRON ..< CHUNK_SIZE + APRON {
		for x in -APRON ..< CHUNK_SIZE + APRON {
			wx := o.x + f32(x)
			wz := o.z + f32(z)
			h := terrain_height(params, wx, wz)
			dhdx := (terrain_height(params, wx + 1, wz) - terrain_height(params, wx - 1, wz)) / 2
			dhdz := (terrain_height(params, wx, wz + 1) - terrain_height(params, wx, wz - 1)) / 2
			slope_fix := math.sqrt(1 + dhdx * dhdx + dhdz * dhdz)
			for y in -APRON ..< CHUNK_SIZE + APRON {
				wy := o.y + f32(y)
				chunk.density[index(x, y, z)] = (wy - h) / slope_fix
			}
		}
	}
}
