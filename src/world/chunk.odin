package world

import "core:math"
import "core:math/linalg"

CHUNK_SIZE :: 32
APRON :: 1
SAMPLES :: CHUNK_SIZE + 2 * APRON
OUTSIDE_DISTANCE :: f32(1)

Chunk :: struct {
	coord:   [3]int,
	density: [SAMPLES * SAMPLES * SAMPLES]f32,
}

index :: proc(x, y, z: int) -> int {
	return (x + APRON) + (y + APRON) * SAMPLES + (z + APRON) * SAMPLES * SAMPLES
}

origin :: proc(coord: [3]int) -> [3]f32 {
	return linalg.array_cast(coord, f32) * CHUNK_SIZE
}

@(private)
sample_local :: proc(chunk: ^Chunk, local: [3]f32) -> f32 {
	x0 := int(math.floor(local.x))
	y0 := int(math.floor(local.y))
	z0 := int(math.floor(local.z))

	LO :: -APRON
	HI :: CHUNK_SIZE + APRON - 1
	if x0 < LO || y0 < LO || z0 < LO || x0 >= HI || y0 >= HI || z0 >= HI {
		return OUTSIDE_DISTANCE
	}

	fx := local.x - f32(x0)
	fy := local.y - f32(y0)
	fz := local.z - f32(z0)

	c000 := chunk.density[index(x0, y0, z0)]
	c100 := chunk.density[index(x0 + 1, y0, z0)]
	c010 := chunk.density[index(x0, y0 + 1, z0)]
	c110 := chunk.density[index(x0 + 1, y0 + 1, z0)]
	c001 := chunk.density[index(x0, y0, z0 + 1)]
	c101 := chunk.density[index(x0 + 1, y0, z0 + 1)]
	c011 := chunk.density[index(x0, y0 + 1, z0 + 1)]
	c111 := chunk.density[index(x0 + 1, y0 + 1, z0 + 1)]

	c00 := math.lerp(c000, c100, fx)
	c10 := math.lerp(c010, c110, fx)
	c01 := math.lerp(c001, c101, fx)
	c11 := math.lerp(c011, c111, fx)

	c0 := math.lerp(c00, c10, fy)
	c1 := math.lerp(c01, c11, fy)

	return math.lerp(c0, c1, fz)
}

GROUND_Y :: f32(4)
SPHERE_CENTER :: [3]f32{16, 10, 16}
SPHERE_RADIUS :: f32(8)

fill_test_scene :: proc(chunk: ^Chunk) {
	o := origin(chunk.coord)
	for z in -APRON ..< CHUNK_SIZE + APRON {
		for y in -APRON ..< CHUNK_SIZE + APRON {
			for x in -APRON ..< CHUNK_SIZE + APRON {
				cell := o + {f32(x), f32(y), f32(z)}
				d_sphere := linalg.distance(cell, SPHERE_CENTER) - SPHERE_RADIUS
				d_ground := cell.y - GROUND_Y
				chunk.density[index(x, y, z)] = min(d_sphere, d_ground)
			}
		}
	}
}
