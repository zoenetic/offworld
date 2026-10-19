package world

import "core:math/linalg"

World :: struct {
	chunks:  map[[3]int]^Chunk,
	terrain: Terrain_Params,
}

make_world :: proc(terrain: Terrain_Params) -> (w: World) {
	w.terrain = terrain
	w.chunks = make(map[[3]int]^Chunk)
	return w
}

destroy_world :: proc(w: ^World) {
	for _, c in w.chunks {
		free(c)
	}
	delete(w.chunks)
}

add_chunk :: proc(w: ^World, coord: [3]int) -> ^Chunk {
	if c, ok := w.chunks[coord]; ok do return c
	c := new(Chunk)
	c.coord = coord
	w.chunks[coord] = c
	return c
}

load_chunk :: proc(w: ^World, coord: [3]int) -> ^Chunk {
	c := add_chunk(w, coord)
	fill_terrain(c, w.terrain)
	return c
}

unload_chunk :: proc(w: ^World, coord: [3]int) {
	if c, ok := w.chunks[coord]; ok do free(c); delete_key(&w.chunks, coord)
}

locate :: proc(p: [3]f32) -> (coord: [3]int, local: [3]f32) {
	coord = linalg.array_cast(linalg.floor(p / CHUNK_SIZE), int)
	local = p - origin(coord)
	return
}

sample :: proc(w: ^World, p: [3]f32) -> f32 {
	coord, local := locate(p)
	if c, ok := w.chunks[coord]; ok do return sample_local(c, local)
	return OUTSIDE_DISTANCE
}

gradient :: proc(w: ^World, p: [3]f32) -> [3]f32 {
	H :: f32(0.5)
	g := [3]f32 {
		sample(w, p + {H, 0, 0}) - sample(w, p - {H, 0, 0}),
		sample(w, p + {0, H, 0}) - sample(w, p - {0, H, 0}),
		sample(w, p + {0, 0, H}) - sample(w, p - {0, 0, H}),
	}
	length := linalg.length(g)
	if length < 1e-6 do return {0, 0, 0}
	return g / length
}
