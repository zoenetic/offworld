package mesh

import "../world"
import "core:math/linalg"

Mesh :: struct {
	positions: [dynamic][3]f32,
	colours:   [dynamic][4]u8,
}

destroy :: proc(m: ^Mesh) {
	delete(m.positions)
	delete(m.colours)
}

SUN_DIR :: [3]f32{0.4, 0.8, 0.3}
AMBIENT :: f32(0.35)
GRASS :: [3]f32{110, 160, 80}
ROCK :: [3]f32{130, 125, 115}

WALKABLE_NORMAL_Y :: f32(0.7)

N :: world.CHUNK_SIZE
CELLS :: N - 1

@(private)
cell_index :: proc(x, y, z: int) -> int {
	return x + y * CELLS + z * CELLS * CELLS
}

build :: proc(chunk: ^world.Chunk) -> (m: Mesh) {
	cell_vertex := make([][3]f32, CELLS * CELLS * CELLS)
	defer delete(cell_vertex)

	for z in 0 ..< CELLS {
		for y in 0 ..< CELLS {
			for x in 0 ..< CELLS {
				d: [8]f32
				inside: bit_set[0 ..< 8]
				for i in 0 ..< 8 {
					o := [3]int{i & 1, (i >> 1) & 1, (i >> 2) & 1}
					d[i] = chunk.density[world.index(x + o.x, y + o.y, z + o.z)]
					if d[i] < 0 do inside += {i}
				}
				if inside == {} || card(inside) == 8 do continue

				sum: [3]f32
				crossings := 0
				for i in 0 ..< 8 {
					for b in ([3]int{1, 2, 4}) {
						if i & b != 0 do continue
						j := i | b
						if (i in inside) == (j in inside) do continue
						t := d[i] / (d[i] - d[j])
						a := [3]f32{f32(i & 1), f32((i >> 1) & 1), f32((i >> 2) & 1)}
						c := [3]f32{f32(j & 1), f32((j >> 1) & 1), f32((j >> 2) & 1)}
						sum += linalg.lerp(a, c, t)
						crossings += 1
					}
				}
				cell_vertex[cell_index(x, y, z)] =
					[3]f32{f32(x), f32(y), f32(z)} + sum / f32(crossings)
			}
		}
	}
	for z in 0 ..< N {
		for y in 0 ..< N {
			for x in 0 ..< N {
				p := [3]int{x, y, z}
				d0 := chunk.density[world.index(x, y, z)]
				for a in 0 ..< 3 {
					u := (a + 1) % 3
					v := (a + 2) % 3
					if p[a] >= CELLS || p[u] < 1 || p[v] < 1 || p[u] >= CELLS || p[v] >= CELLS do continue
					q := p
					q[a] += 1
					d1 := chunk.density[world.index(q.x, q.y, q.z)]
					if (d0 < 0) == (d1 < 0) do continue
					c00, c10, c11, c01 := p, p, p, p
					c00[u] -= 1; c00[v] -= 1
					c10[v] -= 1
					c01[u] -= 1
					v00 := cell_vertex[cell_index(c00.x, c00.y, c00.z)]
					v10 := cell_vertex[cell_index(c10.x, c10.y, c10.z)]
					v11 := cell_vertex[cell_index(c11.x, c11.y, c11.z)]
					v01 := cell_vertex[cell_index(c01.x, c01.y, c01.z)]
					if d0 < 0 {
						emit_triangle(&m, v00, v10, v11)
						emit_triangle(&m, v00, v11, v01)
					} else {
						emit_triangle(&m, v00, v11, v10)
						emit_triangle(&m, v00, v01, v11)
					}
				}
			}

		}
	}
	return
}

@(private)
emit_triangle :: proc(m: ^Mesh, a, b, c: [3]f32) {
	n := linalg.normalize(linalg.cross(b - a, c - a))
	light := AMBIENT + (1 - AMBIENT) * max(0, linalg.dot(n, linalg.normalize(SUN_DIR)))
	base := GRASS if n.y > WALKABLE_NORMAL_Y else ROCK
	rgb := base * light
	colour := [4]u8{u8(rgb.r), u8(rgb.g), u8(rgb.b), 255}
	append(&m.positions, a, b, c)
	append(&m.colours, colour, colour, colour)
}
