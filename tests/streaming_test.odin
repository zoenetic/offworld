package tests

import "../src/world"
import "core:testing"

// Square distance in chunks, as the planner uses it. (world.ring is private, so tests keep their own.)
chunk_ring :: proc(offset: [3]int) -> int {
	return max(abs(offset.x), abs(offset.z))
}

// Mark every coord in the plan as loaded. Planning only looks at which coords exist,
// so a blank chunk is enough: no need to pay for filling it.
pretend_load :: proc(w: ^world.World, p: world.Plan) {
	for coord in p.load do world.add_chunk(w, coord)
}

// An off-origin centre, so a planner that forgets to add the centre back gets caught.
CENTER :: [3]int{5, 0, -2}
SQUARE :: (2 * world.LOAD_RADIUS + 1) * (2 * world.LOAD_RADIUS + 1)

@(test)
empty_world_plans_the_whole_square_nearest_first :: proc(t: ^testing.T) {
	w := world.make_world(world.DEFAULT_TERRAIN)
	defer world.destroy_world(&w)
	p := world.plan_streaming(&w, CENTER)
	defer world.delete_plan(&p)

	testing.expect_value(t, len(p.load), SQUARE)
	testing.expect_value(t, len(p.unload), 0)
	if len(p.load) == 0 do return
	testing.expectf(t, p.load[0] == CENTER, "first load is %v, want the centre %v", p.load[0], CENTER)

	seen := make(map[[3]int]bool)
	defer delete(seen)
	previous := 0
	for coord, i in p.load {
		ring := chunk_ring(coord - CENTER)
		if !testing.expectf(t, ring <= world.LOAD_RADIUS, "load[%d] = %v is outside the square", i, coord) do return
		if !testing.expectf(t, coord.y == 0, "load[%d] = %v: not in layer 0", i, coord) do return
		if !testing.expectf(t, coord not_in seen, "load[%d] = %v appears twice", i, coord) do return
		if !testing.expectf(t, ring >= previous, "load[%d] = %v is nearer than the one before it", i, coord) do return
		seen[coord] = true
		previous = ring
	}
}

@(test)
fully_loaded_plans_nothing :: proc(t: ^testing.T) {
	w := world.make_world(world.DEFAULT_TERRAIN)
	defer world.destroy_world(&w)
	first := world.plan_streaming(&w, CENTER)
	pretend_load(&w, first)
	world.delete_plan(&first)

	p := world.plan_streaming(&w, CENTER)
	defer world.delete_plan(&p)
	testing.expectf(t, len(p.load) == 0, "load %v, want nothing", p.load[:])
	testing.expectf(t, len(p.unload) == 0, "unload %v, want nothing", p.unload[:])
}

@(test)
moving_one_chunk_loads_the_leading_edge_and_unloads_nothing :: proc(t: ^testing.T) {
	w := world.make_world(world.DEFAULT_TERRAIN)
	defer world.destroy_world(&w)
	first := world.plan_streaming(&w, CENTER)
	pretend_load(&w, first)
	world.delete_plan(&first)

	moved := CENTER + {1, 0, 0}
	p := world.plan_streaming(&w, moved)
	defer world.delete_plan(&p)

	// Exactly the new column at moved.x + R.
	testing.expect_value(t, len(p.load), 2 * world.LOAD_RADIUS + 1)
	for coord in p.load {
		if !testing.expectf(t, coord.x == moved.x + world.LOAD_RADIUS, "loaded %v, not on the leading edge", coord) do return
	}
	// The trailing column is now R + 1 away: inside the hysteresis ring, so it stays.
	testing.expectf(t, len(p.unload) == 0, "unload %v, want nothing (hysteresis)", p.unload[:])
}

@(test)
moving_two_chunks_unloads_the_far_column :: proc(t: ^testing.T) {
	w := world.make_world(world.DEFAULT_TERRAIN)
	defer world.destroy_world(&w)
	first := world.plan_streaming(&w, CENTER)
	pretend_load(&w, first)
	world.delete_plan(&first)
	step := world.plan_streaming(&w, CENTER + {1, 0, 0})
	pretend_load(&w, step)
	world.delete_plan(&step)

	moved := CENTER + {2, 0, 0}
	p := world.plan_streaming(&w, moved)
	defer world.delete_plan(&p)

	testing.expect_value(t, len(p.load), 2 * world.LOAD_RADIUS + 1)
	testing.expect_value(t, len(p.unload), 2 * world.LOAD_RADIUS + 1)
	for coord in p.unload {
		if !testing.expectf(t, coord.x == CENTER.x - world.LOAD_RADIUS, "unloading %v, want only the original far column", coord) do return
	}
}

@(test)
planning_does_not_change_the_world :: proc(t: ^testing.T) {
	w := world.make_world(world.DEFAULT_TERRAIN)
	defer world.destroy_world(&w)
	world.add_chunk(&w, {100, 0, 100}) // far away: will be in the unload list
	p := world.plan_streaming(&w, CENTER)
	defer world.delete_plan(&p)
	testing.expect_value(t, len(w.chunks), 1)
	testing.expect(t, [3]int{100, 0, 100} in w.chunks, "planning removed a chunk")
}

@(test)
unload_chunk_frees_it_and_ignores_missing :: proc(t: ^testing.T) {
	w := world.make_world(world.DEFAULT_TERRAIN)
	defer world.destroy_world(&w)
	world.add_chunk(&w, {1, 0, 1})
	world.unload_chunk(&w, {1, 0, 1})
	testing.expect_value(t, len(w.chunks), 0)
	world.unload_chunk(&w, {9, 0, 9}) // not loaded: must be a harmless no-op
	testing.expect_value(t, len(w.chunks), 0)
	// The leak tracker checks the freed chunk: if unload_chunk only forgot it, the test reports a leak.
}
