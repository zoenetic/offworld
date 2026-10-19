package tests

import "../src/world"
import "core:math/linalg"
import "core:testing"

@(test)
sample_interpolates_between_cells :: proc(t: ^testing.T) {
	w := new_test_world()
	defer world.destroy_world(&w)
	chunk := w.chunks[{0, 0, 0}]
	below := chunk.density[world.index(2, 4, 2)]
	above := chunk.density[world.index(2, 5, 2)]
	g := world.sample(&w, {2, 4.5, 2})
	testing.expectf(t, approx(g, (below + above) / 2), "got %v, want %v", g, (below + above) / 2)
}

@(test)
sample_outside_is_air :: proc(t: ^testing.T) {
	w := new_test_world()
	defer world.destroy_world(&w)
	a := world.sample(&w, {-5, 10, -5})
	b := world.sample(&w, {40, 10, 40})
	want := world.OUTSIDE_DISTANCE
	testing.expectf(t, a == want, "got %v, want %v", a, want)
	testing.expectf(t, b == want, "got %v, want %v", b, want)
}

@(test)
sample_negative_coord_uses_floor :: proc(t: ^testing.T) {
	w := new_test_world()
	defer world.destroy_world(&w)
	g := world.sample(&w, {-0.5, 4, 2})
	want := world.OUTSIDE_DISTANCE
	testing.expectf(t, g == want, "got %v, want %v", g, want)
}

@(test)
sample_first_layer_is_inside_chunk :: proc(t: ^testing.T) {
	w := new_test_world()
	defer world.destroy_world(&w)
	g := world.sample(&w, {0.5, 0.5, 0.5})
	testing.expectf(t, g < 0, "got %v, wanted a negative value", g)
}

@(test)
gradient_on_ground_points_up :: proc(t: ^testing.T) {
	w := new_test_world()
	defer world.destroy_world(&w)
	g := world.gradient(&w, {2, 4, 2})
	want := [3]f32{0, 1, 0}
	testing.expectf(t, g == want, "got %v, want %v", g, want)
}

@(test)
gradient_on_sphere_points_outward :: proc(t: ^testing.T) {
	w := new_test_world()
	defer world.destroy_world(&w)
	p := [3]f32{24, 12, 16}
	g := world.gradient(&w, p)
	want := linalg.normalize(p - world.SPHERE_CENTER)
	testing.expectf(t, linalg.dot(want, g) > 0.99, "got %v, want direction %v", g, want)
}
