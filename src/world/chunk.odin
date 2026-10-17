package world

import "core:math"
import "core:math/linalg"

CHUNK_SIZE :: 32

Chunk :: struct {
	density: [CHUNK_SIZE * CHUNK_SIZE * CHUNK_SIZE]f32,
}

Index :: int

index :: proc(x, y, z: int) -> Index {
	return x + y * CHUNK_SIZE + z * CHUNK_SIZE * CHUNK_SIZE
}

x :: proc(i: Index) -> (x: int) {
	return i % CHUNK_SIZE
}

y :: proc(i: Index) -> (y: int) {
	return i / CHUNK_SIZE % CHUNK_SIZE
}

z :: proc(i: Index) -> (z: int) {
	return i / (CHUNK_SIZE * CHUNK_SIZE)
}

fill_sphere :: proc(chunk: ^Chunk, center: [3]f32, radius: f32) {
	for z in 0 ..< CHUNK_SIZE {
		for y in 0 ..< CHUNK_SIZE {
			for x in 0 ..< CHUNK_SIZE {
				pos: [3]f32 = {f32(x), f32(y), f32(z)}
				density := linalg.distance(pos, center) - radius
				i := index(x, y, z)
				chunk.density[i] = density
			}
		}
	}
}

OUTSIDE_DISTANCE :: f32(1)

sample :: proc(chunk: ^Chunk, p: [3]f32) -> f32 {
	x0 := int(math.floor(p.x))
	y0 := int(math.floor(p.y))
	z0 := int(math.floor(p.z))

	if x0 < 0 ||
	   y0 < 0 ||
	   z0 < 0 ||
	   x0 >= CHUNK_SIZE - 1 ||
	   y0 >= CHUNK_SIZE - 1 ||
	   z0 >= CHUNK_SIZE - 1 {
		return OUTSIDE_DISTANCE
	}

	fx := p.x - f32(x0)
	fy := p.y - f32(y0)
	fz := p.z - f32(z0)

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

gradient :: proc(chunk: ^Chunk, p: [3]f32) -> [3]f32 {
	H :: f32(0.5)
	g := [3]f32 {
		sample(chunk, p + {H, 0, 0}) - sample(chunk, p - {H, 0, 0}),
		sample(chunk, p + {0, H, 0}) - sample(chunk, p - {0, H, 0}),
		sample(chunk, p + {0, 0, H}) - sample(chunk, p - {0, 0, H}),
	}
	length := linalg.length(g)
	if length < 1e-6 {
		return {0, 0, 0}
	}
	return g / length
}

GROUND_Y :: f32(4)
SPHERE_CENTER :: [3]f32{16, 10, 16}
SPHERE_RADIUS :: f32(8)

fill_test_scene :: proc(chunk: ^Chunk) {
	for z in 0 ..< CHUNK_SIZE {
		for y in 0 ..< CHUNK_SIZE {
			for x in 0 ..< CHUNK_SIZE {
				p := [3]f32{f32(x), f32(y), f32(z)}
				d_sphere := linalg.distance(p, SPHERE_CENTER) - SPHERE_RADIUS
				d_ground := p.y - GROUND_Y
				chunk.density[index(x, y, z)] = min(d_sphere, d_ground)
			}
		}
	}
}
