package world

import "core:math/linalg"

RAY_MAX_STEPS :: 128
RAY_HIT_EPS :: f32(0.01)
RAY_MIN_STEP :: f32(0.05)

raycast :: proc(chunk: ^Chunk, origin, dir: [3]f32, max_dist: f32) -> (hit: [3]f32, ok: bool) {
	t: f32 = 0
	for _ in 1 ..= RAY_MAX_STEPS {
		p := origin + dir * t
		d := sample(chunk, p)
		if d < RAY_HIT_EPS do return p, true
		t += max(d, RAY_MIN_STEP)
		if t > max_dist do break
	}
	return {}, false
}

@(private)
Tool :: enum {
	Build,
	Dig,
}

@(private)
brush :: proc(chunk: ^Chunk, center: [3]f32, radius: f32, tool: Tool) {
	lo := linalg.array_cast(linalg.floor(center - radius - 1), int)
	hi := linalg.array_cast(linalg.ceil(center + radius + 1), int)
	lo = linalg.clamp(lo, 0, CHUNK_SIZE)
	hi = linalg.clamp(hi, 0, CHUNK_SIZE)
	for z in lo.z ..< hi.z {
		for y in lo.y ..< hi.y {
			for x in lo.x ..< hi.x {
				cell := [3]f32{f32(x), f32(y), f32(z)}
				brush_distance := linalg.distance(cell, center) - radius
				i := index(x, y, z)
				density := chunk.density[i]
				switch tool {
				case .Build:
					chunk.density[i] = min(density, brush_distance)
				case .Dig:
					chunk.density[i] = max(density, -brush_distance)
				}

			}
		}
	}
}

build :: proc(chunk: ^Chunk, center: [3]f32, radius: f32) {
	brush(chunk, center, radius, tool = .Build)
}

dig :: proc(chunk: ^Chunk, center: [3]f32, radius: f32) {
	brush(chunk, center, radius, tool = .Dig)
}
