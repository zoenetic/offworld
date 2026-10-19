package tests

import "../src/world"
import "core:testing"

@(test)
locate_handles_negative_positions :: proc(t: ^testing.T) {
	Case :: struct {
		p:     [3]f32,
		coord: [3]int,
		local: [3]f32,
	}
	cases := [?]Case {
		{{-0.5, 0, 0}, {-1, 0, 0}, {31.5, 0, 0}}, // integer `/` would put this in chunk 0
		{{32, 0, 0}, {1, 0, 0}, {0, 0, 0}},
		{{-32, 0, 0}, {-1, 0, 0}, {0, 0, 0}},
		{{-33, 5, 64}, {-2, 0, 2}, {31, 5, 0}},
	}
	for c in cases {
		coord, local := world.locate(c.p)
		testing.expectf(t, coord == c.coord, "locate(%v): coord %v, want %v", c.p, coord, c.coord)
		testing.expectf(t, local == c.local, "locate(%v): local %v, want %v", c.p, local, c.local)
	}
}

@(test)
padding_matches_neighbour :: proc(t: ^testing.T) {
	w := world.make_world(world.DEFAULT_TERRAIN)
	defer world.destroy_world(&w)
	a := world.load_chunk(&w, {0, 0, 0})
	b := world.load_chunk(&w, {1, 0, 0})
	// Exact equality, no tolerance: both values come from the same formula at the same position.
	for z in -world.APRON ..< world.CHUNK_SIZE + world.APRON {
		for y in -world.APRON ..< world.CHUNK_SIZE + world.APRON {
			a_pad, b_own := a.density[world.index(world.CHUNK_SIZE, y, z)], b.density[world.index(0, y, z)]
			if !testing.expectf(t, a_pad == b_own, "y=%d z=%d: a's padding %v != b's first sample %v", y, z, a_pad, b_own) do return
			b_pad, a_own := b.density[world.index(-1, y, z)], a.density[world.index(world.CHUNK_SIZE - 1, y, z)]
			if !testing.expectf(t, b_pad == a_own, "y=%d z=%d: b's padding %v != a's last sample %v", y, z, b_pad, a_own) do return
		}
	}
}

@(test)
sample_is_continuous_across_border :: proc(t: ^testing.T) {
	w := world.make_world(world.DEFAULT_TERRAIN)
	defer world.destroy_world(&w)
	world.load_chunk(&w, {0, 0, 0})
	world.load_chunk(&w, {1, 0, 0})
	for z in 0 ..< world.CHUNK_SIZE {
		for y in 0 ..< world.CHUNK_SIZE {
			p := [3]f32{32, f32(y) + 0.5, f32(z) + 0.5}
			before := world.sample(&w, p - {0.001, 0, 0})
			after := world.sample(&w, p + {0.001, 0, 0})
			if !testing.expectf(t, approx(before, after, 0.01), "jump at %v: %v -> %v", p, before, after) do return
		}
	}
}

@(test)
unloaded_chunk_is_air :: proc(t: ^testing.T) {
	w := world.make_world(world.DEFAULT_TERRAIN)
	defer world.destroy_world(&w)
	world.load_chunk(&w, {0, 0, 0})
	got := world.sample(&w, {200, 10, 200})
	testing.expectf(t, got == world.OUTSIDE_DISTANCE, "got %v, want %v", got, world.OUTSIDE_DISTANCE)
}

@(test)
load_chunk_twice_reuses_it :: proc(t: ^testing.T) {
	w := world.make_world(world.DEFAULT_TERRAIN)
	defer world.destroy_world(&w)
	first := world.load_chunk(&w, {3, 0, -2})
	second := world.load_chunk(&w, {3, 0, -2})
	testing.expect(t, first == second, "loaded a second copy of the same chunk")
	testing.expect_value(t, len(w.chunks), 1)
}
