package tests

import "../src/player"
import "../src/world"
import "core:math/linalg"
import "core:testing"

@(test)
settles_on_ground :: proc(t: ^testing.T) {
	chunk := new_test_world()
	defer free(chunk)
	p := settled_player(chunk, {4, 8, 4})
	if !testing.expect(t, p.grounded, "setup: player never settled") do return
	tick(&p, chunk, {}, 120)
	w := world.GROUND_Y + player.PLAYER_RADIUS
	a := p.position.y
	b := p.grounded
	v := p.velocity.y
	testing.expectf(t, approx(a, w), "got %v, want approximately %v", a, w)
	testing.expectf(t, b, "got %v, wanted true", b)
	testing.expectf(t, approx(v, 0), "got %v, wanted approximately %v", v, 0)
}

@(test)
standing_still_does_not_drift :: proc(t: ^testing.T) {
	chunk := new_test_world()
	defer free(chunk)
	p := settled_player(chunk, {4, 8, 4})
	if !testing.expect(t, p.grounded, "setup: player never settled") do return
	start := p.position
	for i in 0 ..< 600 {
		player.update(&p, chunk, {}, DT)
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
	chunk := new_test_world()
	defer free(chunk)
	p := settled_player(chunk, {4, 8, 4})
	if !testing.expect(t, p.grounded, "setup: player never settled") do return
	w: f32 = p.position.y + 1.58
	g := p.position.y
	player.update(&p, chunk, {jump = true}, DT)
	for _ in 0 ..< 120 {
		player.update(&p, chunk, {}, DT)
		g = max(g, p.position.y)
	}
	testing.expectf(t, approx(g, w, 0.05), "got %v, want approximately %v", g, w)
	testing.expectf(t, p.grounded, "got %v expected grounded to be true", p.grounded)
}

@(test)
no_air_jump :: proc(t: ^testing.T) {
	chunk := new_test_world()
	defer free(chunk)
	p := settled_player(chunk, {4, 8, 4})
	player.update(&p, chunk, {jump = true}, DT)
	if !testing.expect(t, p.velocity.y > 0, "setup: didn't jump") do return
	w := p.velocity.y
	for i in 0 ..< 10 {
		player.update(&p, chunk, {jump = true}, DT)
		if !testing.expectf(t, p.velocity.y < w, "gained velocity at tick %d", i) do return
	}
}

@(test)
walk_follows_yaw :: proc(t: ^testing.T) {
	chunk := new_test_world()
	defer free(chunk)
	x, y, z: f32 = 10, 10, 10
	p := settled_player(chunk, {x, y, z})
	s := p.position
	tick(&p, chunk, {forward = 1}, 60)
	d := p.position - s
	testing.expectf(t, approx(d.x, 0), "got dx %v, want approximately %v", d.x, 0)
	testing.expectf(t, approx(d.z, -6), "got dz %v, want approximately %v", d.z, -6)
	testing.expectf(t, p.grounded, "got %v expected grounded to be true", p.grounded)
}

@(test)
diagonal_is_not_faster :: proc(t: ^testing.T) {
	chunk := new_test_world()
	defer free(chunk)
	p := settled_player(chunk, {4, 8, 10})
	s := p.position
	tick(&p, chunk, {forward = 1, right = 1}, 60)
	g := linalg.distance(s.xz, p.position.xz)
	testing.expectf(t, approx(g, 6), "got %v, want approximately %v", g, 6)
	testing.expectf(t, p.grounded, "got %v expected grounded to be true", p.grounded)
}

@(test)
no_input_means_no_movement :: proc(t: ^testing.T) {
	g := player.movement_direction({}, 3)
	w: [3]f32 = {0, 0, 0}
	testing.expectf(t, g == w, "got %v, want %v", g, w)
}

@(test)
sphere_blocks_movement :: proc(t: ^testing.T) {
	chunk := new_test_world()
	defer free(chunk)
	p := settled_player(chunk, {16, 8, 2})
	p.yaw = 0
	tick(&p, chunk, {forward = 1}, 300)
	min_z: f32 = 8
	max_z: f32 = 10
	z := p.position.z
	testing.expectf(t, z > min_z, "got %v, want > %v", z, min_z)
	testing.expectf(t, z < max_z, "got %v, want < %v", z, max_z)
}

@(test)
max_fall_is_clamped :: proc(t: ^testing.T) {
	chunk := new_test_world()
	defer free(chunk)
	p := player.Player {
		position = {4, 30, 40},
	}
	for _ in 0 ..< 100 {
		player.update(&p, chunk, {}, DT)
		v := p.velocity.y
		if !testing.expectf(t, v >= -player.MAX_FALL, "got %v, want <= %v", v, player.MAX_FALL) do return
	}
}
