package tests

import "../src/mesh"
import "../src/world"
import "core:math/linalg"
import "core:testing"

// Fill a chunk's whole STORED range (padding included) from a function of world position.
fill_with :: proc(chunk: ^world.Chunk, f: proc(p: [3]f32) -> f32) {
	o := world.origin(chunk.coord)
	for z in -world.APRON ..< world.CHUNK_SIZE + world.APRON {
		for y in -world.APRON ..< world.CHUNK_SIZE + world.APRON {
			for x in -world.APRON ..< world.CHUNK_SIZE + world.APRON {
				chunk.density[world.index(x, y, z)] = f(o + {f32(x), f32(y), f32(z)})
			}
		}
	}
}

flat_ground :: proc(p: [3]f32) -> f32 {return p.y - 4.3}

triangle_normal :: proc(m: mesh.Mesh, tri: int) -> [3]f32 {
	a, b, c := m.positions[tri * 3], m.positions[tri * 3 + 1], m.positions[tri * 3 + 2]
	return linalg.normalize(linalg.cross(b - a, c - a))
}

x_range :: proc(m: mesh.Mesh) -> (lo, hi: f32) {
	lo, hi = max(f32), min(f32)
	for p in m.positions {
		lo = min(lo, p.x)
		hi = max(hi, p.x)
	}
	return
}

@(test)
all_air_or_all_solid_makes_no_triangles :: proc(t: ^testing.T) {
	w := world.make_world(world.DEFAULT_TERRAIN)
	defer world.destroy_world(&w)
	chunk := world.add_chunk(&w, {0, 0, 0})

	fill_with(chunk, proc(p: [3]f32) -> f32 {return 1})
	air := mesh.build(chunk)
	defer mesh.destroy(&air)
	testing.expectf(t, len(air.positions) == 0, "all air: got %d vertices, want 0", len(air.positions))

	fill_with(chunk, proc(p: [3]f32) -> f32 {return -1})
	solid := mesh.build(chunk)
	defer mesh.destroy(&solid)
	testing.expectf(t, len(solid.positions) == 0, "all solid: got %d vertices, want 0", len(solid.positions))
}

@(test)
flat_ground_is_flat_and_faces_up :: proc(t: ^testing.T) {
	w := world.make_world(world.DEFAULT_TERRAIN)
	defer world.destroy_world(&w)
	chunk := world.add_chunk(&w, {0, 0, 0})
	fill_with(chunk, flat_ground)
	m := mesh.build(chunk)
	defer mesh.destroy(&m)

	// One quad (2 triangles) per vertical edge the chunk owns: 32 x 32.
	testing.expect_value(t, len(m.positions) / 3, 2 * world.CHUNK_SIZE * world.CHUNK_SIZE)
	testing.expect_value(t, len(m.colours), len(m.positions))
	for p, i in m.positions {
		if !testing.expectf(t, approx(p.y, 4.3), "vertex %d at y=%v, want 4.3", i, p.y) do return
	}
	for i in 0 ..< len(m.positions) / 3 {
		n := triangle_normal(m, i)
		if !testing.expectf(t, linalg.dot(n, [3]f32{0, 1, 0}) > 0.999, "triangle %d faces %v, want up", i, n) do return
	}
}

@(test)
sphere_vertices_on_surface_and_normals_outward :: proc(t: ^testing.T) {
	w := world.make_world(world.DEFAULT_TERRAIN)
	defer world.destroy_world(&w)
	chunk := world.add_chunk(&w, {0, 0, 0})
	fill_with(chunk, proc(p: [3]f32) -> f32 {return linalg.distance(p, [3]f32{15.5, 15.5, 15.5}) - 9})
	m := mesh.build(chunk)
	defer mesh.destroy(&m)

	center := [3]f32{15.5, 15.5, 15.5}
	if !testing.expect(t, len(m.positions) > 0, "setup: sphere produced no triangles") do return
	for p, i in m.positions {
		off := abs(linalg.distance(p, center) - 9)
		if !testing.expectf(t, off < 0.1, "vertex %d is %v off the surface", i, off) do return
	}
	for i in 0 ..< len(m.positions) / 3 {
		mid := (m.positions[i * 3] + m.positions[i * 3 + 1] + m.positions[i * 3 + 2]) / 3
		n := triangle_normal(m, i)
		if !testing.expectf(t, linalg.dot(n, mid - center) > 0, "triangle %d faces inward", i) do return
	}
}

@(test)
mesh_lands_at_chunk_origin :: proc(t: ^testing.T) {
	w := world.make_world(world.DEFAULT_TERRAIN)
	defer world.destroy_world(&w)
	chunk := world.add_chunk(&w, {-2, 0, 1})
	fill_with(chunk, flat_ground)
	m := mesh.build(chunk)
	defer mesh.destroy(&m)

	// Cubes -1 ..< 32 have their vertex at the cube's middle: origin - 0.5 .. origin + 31.5.
	o := world.origin(chunk.coord)
	lo, hi := x_range(m)
	testing.expectf(t, approx(lo, o.x - 0.5) && approx(hi, o.x + 31.5), "x %v..%v, want %v..%v", lo, hi, o.x - 0.5, o.x + 31.5)
	for p, i in m.positions {
		if !testing.expectf(t, p.z >= o.z - 0.5 && p.z <= o.z + 31.5, "vertex %d at z=%v, outside its chunk", i, p.z) do return
	}
}

@(test)
neighbouring_chunks_meet_without_a_gap :: proc(t: ^testing.T) {
	w := world.make_world(world.DEFAULT_TERRAIN)
	defer world.destroy_world(&w)
	a_chunk := world.add_chunk(&w, {0, 0, 0})
	b_chunk := world.add_chunk(&w, {1, 0, 0})
	fill_with(a_chunk, flat_ground)
	fill_with(b_chunk, flat_ground)
	a := mesh.build(a_chunk)
	defer mesh.destroy(&a)
	b := mesh.build(b_chunk)
	defer mesh.destroy(&b)

	_, a_hi := x_range(a)
	b_lo, _ := x_range(b)
	testing.expectf(t, approx(a_hi, 31.5) && approx(b_lo, 31.5), "chunk 0 ends at x=%v, chunk 1 starts at x=%v: want both 31.5", a_hi, b_lo)
}

// Every edge of a closed surface is shared by exactly two triangles. Combine two neighbouring
// terrain meshes: an edge used by only ONE triangle along their shared border is a crack.
@(test)
terrain_border_is_watertight :: proc(t: ^testing.T) {
	Edge :: [2][3]f32
	edge_key :: proc(p, q: [3]f32) -> Edge {
		// order the endpoints so p→q and q→p count as the same edge
		if p.x < q.x || (p.x == q.x && (p.y < q.y || (p.y == q.y && p.z < q.z))) do return {p, q}
		return {q, p}
	}

	w := world.make_world(world.DEFAULT_TERRAIN)
	defer world.destroy_world(&w)
	a := mesh.build(world.load_chunk(&w, {0, 0, 0}))
	defer mesh.destroy(&a)
	b := mesh.build(world.load_chunk(&w, {1, 0, 0}))
	defer mesh.destroy(&b)

	uses := make(map[Edge]int)
	defer delete(uses)
	for m in ([2]^mesh.Mesh{&a, &b}) {
		for i := 0; i < len(m.positions); i += 3 {
			p0, p1, p2 := m.positions[i], m.positions[i + 1], m.positions[i + 2]
			uses[edge_key(p0, p1)] += 1
			uses[edge_key(p1, p2)] += 1
			uses[edge_key(p2, p0)] += 1
		}
	}

	// The shared border runs along x = 32; stay clear of the outer rim in z (open by design).
	cracks := 0
	for e, n in uses {
		near_border := e[0].x > 30 && e[0].x < 33 && e[1].x > 30 && e[1].x < 33
		inside_z := e[0].z > 0 && e[0].z < 31 && e[1].z > 0 && e[1].z < 31
		if n == 1 && near_border && inside_z do cracks += 1
	}
	testing.expectf(t, cracks == 0, "%d open edges along the chunk border", cracks)
}
