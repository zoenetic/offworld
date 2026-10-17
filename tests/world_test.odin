package tests

import "../src/world"
import "core:math/linalg"
import "core:testing"

@(test)
sample_interpolates_between_cells :: proc(t: ^testing.T) {
	chunk := new_test_world()
	defer free(chunk)
	below := chunk.density[world.index(2, 4, 2)]
	above := chunk.density[world.index(2, 5, 2)]
	g := world.sample(chunk, {2, 4.5, 2})
	testing.expectf(t, approx(g, (below + above) / 2), "got %v, want %v", g, (below + above) / 2)
}

@(test)
sample_outside_is_air :: proc(t: ^testing.T) {
	chunk := new_test_world()
	defer free(chunk)
	a := world.sample(chunk, {-5, 10, -5})
	b := world.sample(chunk, {40, 10, 40})
	w := world.OUTSIDE_DISTANCE
	testing.expectf(t, a == w, "got %v, want %v", a, w)
	testing.expectf(t, b == w, "got %v, want %v", b, w)
}

@(test)
sample_negative_coord_uses_floor :: proc(t: ^testing.T) {
	chunk := new_test_world()
	defer free(chunk)
	g := world.sample(chunk, {-0.5, 4, 2})
	w := world.OUTSIDE_DISTANCE
	testing.expectf(t, g == world.OUTSIDE_DISTANCE, "got %v, want %v", g, w)
}

@(test)
sample_first_layer_is_inside_chunk :: proc(t: ^testing.T) {
	chunk := new_test_world()
	defer free(chunk)
	g := world.sample(chunk, {0.5, 0.5, 0.5})
	testing.expectf(t, g < 0, "got %v, wanted a negative value", g)
}

@(test)
gradient_on_ground_points_up :: proc(t: ^testing.T) {
	chunk := new_test_world()
	defer free(chunk)
	g := world.gradient(chunk, {2, 4, 2})
	w := [3]f32{0, 1, 0}
	testing.expectf(t, g == w, "got %v, want %v", g, w)
}

@(test)
gradient_on_sphere_points_outward :: proc(t: ^testing.T) {
	chunk := new_test_world()
	defer free(chunk)
	p := [3]f32{24, 12, 16}
	g := world.gradient(chunk, p)
	w := linalg.normalize(p - world.SPHERE_CENTER)
	testing.expectf(t, linalg.dot(w, g) > 0.99, "got %v, want direction %v", g, w)
}
