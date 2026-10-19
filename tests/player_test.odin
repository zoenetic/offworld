package tests

import "../src/player"
import "../src/world"
import "core:math"
import "core:math/linalg"
import "core:testing"

// A plane rising `rise` per unit of x, in a single chunk at the origin.
// Dividing by sqrt(1 + rise²) makes the value a true distance.
fill_slope :: proc(w: ^world.World, rise: f32) {
	chunk := world.add_chunk(w, {0, 0, 0})
	for z in -world.APRON ..< world.CHUNK_SIZE + world.APRON {
		for y in -world.APRON ..< world.CHUNK_SIZE + world.APRON {
			for x in -world.APRON ..< world.CHUNK_SIZE + world.APRON {
				chunk.density[world.index(x, y, z)] =
					(f32(y) - rise * f32(x) - 4) / math.sqrt(1 + rise * rise)
			}
		}
	}
}

@(test)
no_creep_on_walkable_slope :: proc(t: ^testing.T) {
	w := world.make_world(world.DEFAULT_TERRAIN)
	defer world.destroy_world(&w)
	fill_slope(&w, 0.5) // about 26.6°: walkable
	p := settled_player(&w, {16, 16, 16})
	if !testing.expect(t, p.grounded, "setup: player never settled") do return
	start := p.position
	tick(&p, &w, {}, 300)
	drift := linalg.distance(start.xz, p.position.xz)
	testing.expectf(t, drift < 0.01, "crept %v units downhill in 5 s", drift)
}

@(test)
slides_off_steep_slope :: proc(t: ^testing.T) {
	w := world.make_world(world.DEFAULT_TERRAIN)
	defer world.destroy_world(&w)
	fill_slope(&w, 1.5) // about 56°: too steep to stand on
	p := player.Player {
		position = {16, 30, 16},
	}
	tick(&p, &w, {}, 60)
	start := p.position
	for i in 0 ..< 60 {
		player.update(&p, &w, {}, DT)
		if !testing.expectf(t, !p.grounded, "stood on a 56° slope at tick %d", i) do return
	}
	// The slope rises toward +x, so downhill is -x.
	testing.expectf(
		t,
		p.position.x < start.x - 0.25,
		"didn't slide downhill: x %v -> %v",
		start.x,
		p.position.x,
	)
}

@(test)
settles_on_ground :: proc(t: ^testing.T) {
	w := new_test_world()
	defer world.destroy_world(&w)
	p := settled_player(&w, {4, 8, 4})
	if !testing.expect(t, p.grounded, "setup: player never settled") do return
	tick(&p, &w, {}, 120)
	want := world.GROUND_Y + player.RADIUS
	a := p.position.y
	b := p.grounded
	v := p.velocity.y
	testing.expectf(t, approx(a, want), "got %v, want approximately %v", a, want)
	testing.expectf(t, b, "got %v, wanted true", b)
	testing.expectf(t, approx(v, 0), "got %v, wanted approximately %v", v, 0)
}

@(test)
standing_still_does_not_drift :: proc(t: ^testing.T) {
	w := new_test_world()
	defer world.destroy_world(&w)
	p := settled_player(&w, {4, 8, 4})
	if !testing.expect(t, p.grounded, "setup: player never settled") do return
	start := p.position
	for i in 0 ..< 600 {
		player.update(&p, &w, {}, DT)
		if !testing.expectf(t, p.grounded, "lost the ground at tick %d", i) do return
	}
	testing.expectf(
		t,
		approx(p.position.x, start.x) && approx(p.position.z, start.z),
		"drifted from %v to %v",
		start,
		p.position,
	)
}

@(test)
jump_goes_up_then_lands :: proc(t: ^testing.T) {
	w := new_test_world()
	defer world.destroy_world(&w)
	p := settled_player(&w, {4, 8, 4})
	if !testing.expect(t, p.grounded, "setup: player never settled") do return
	want: f32 = p.position.y + 1.58
	g := p.position.y
	player.update(&p, &w, {jump = true}, DT)
	for _ in 0 ..< 120 {
		player.update(&p, &w, {}, DT)
		g = max(g, p.position.y)
	}
	testing.expectf(t, approx(g, want, 0.05), "got %v, want approximately %v", g, want)
	testing.expectf(t, p.grounded, "got %v expected grounded to be true", p.grounded)
}

@(test)
no_air_jump :: proc(t: ^testing.T) {
	w := new_test_world()
	defer world.destroy_world(&w)
	p := settled_player(&w, {4, 8, 4})
	player.update(&p, &w, {jump = true}, DT)
	if !testing.expect(t, p.velocity.y > 0, "setup: didn't jump") do return
	first := p.velocity.y
	for i in 0 ..< 10 {
		player.update(&p, &w, {jump = true}, DT)
		if !testing.expectf(t, p.velocity.y < first, "gained velocity at tick %d", i) do return
	}
}

@(test)
walk_follows_yaw :: proc(t: ^testing.T) {
	w := new_test_world()
	defer world.destroy_world(&w)
	x, y, z: f32 = 10, 10, 10
	p := settled_player(&w, {x, y, z})
	s := p.position
	tick(&p, &w, {forward = 1}, 60)
	d := p.position - s
	testing.expectf(t, approx(d.x, 0), "got dx %v, want approximately %v", d.x, 0)
	testing.expectf(t, approx(d.z, -6), "got dz %v, want approximately %v", d.z, -6)
	testing.expectf(t, p.grounded, "got %v expected grounded to be true", p.grounded)
}

@(test)
diagonal_is_not_faster :: proc(t: ^testing.T) {
	w := new_test_world()
	defer world.destroy_world(&w)
	p := settled_player(&w, {4, 8, 10})
	s := p.position
	tick(&p, &w, {forward = 1, right = 1}, 60)
	g := linalg.distance(s.xz, p.position.xz)
	testing.expectf(t, approx(g, 6), "got %v, want approximately %v", g, 6)
	testing.expectf(t, p.grounded, "got %v expected grounded to be true", p.grounded)
}

@(test)
no_input_means_no_movement :: proc(t: ^testing.T) {
	g := player.movement_direction({}, 3)
	want: [3]f32 = {0, 0, 0}
	testing.expectf(t, g == want, "got %v, want %v", g, want)
}

@(test)
sphere_blocks_movement :: proc(t: ^testing.T) {
	w := new_test_world()
	defer world.destroy_world(&w)
	p := settled_player(&w, {16, 8, 2})
	p.yaw = 0
	tick(&p, &w, {forward = 1}, 300)
	min_z: f32 = 8
	max_z: f32 = 10
	z := p.position.z
	testing.expectf(t, z > min_z, "got %v, want > %v", z, min_z)
	testing.expectf(t, z < max_z, "got %v, want < %v", z, max_z)
}

@(test)
max_fall_is_clamped :: proc(t: ^testing.T) {
	w := new_test_world()
	defer world.destroy_world(&w)
	p := player.Player {
		position = {4, 30, 40},
	}
	for _ in 0 ..< 100 {
		player.update(&p, &w, {}, DT)
		v := p.velocity.y
		if !testing.expectf(t, v >= -player.MAX_FALL_SPEED, "got %v, want <= %v", v, player.MAX_FALL_SPEED) do return
	}
}
