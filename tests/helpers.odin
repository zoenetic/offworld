package tests

import "../src/player"
import "../src/world"
import "core:math"

DT :: f32(1.0 / 60.0)

approx :: proc(a, b: f32, eps := f32(1e-3)) -> bool {
	return math.abs(a - b) <= eps
}

// A World with one chunk at the origin, filled with the test scene (flat ground + sphere).
// Everywhere else is unloaded, so it's air. Caller must `defer world.destroy_world(&w)`.
new_test_world :: proc() -> world.World {
	w := world.make_world(world.DEFAULT_TERRAIN)
	world.fill_test_scene(world.add_chunk(&w, {0, 0, 0}))
	return w
}

tick :: proc(p: ^player.Player, w: ^world.World, intent: player.Intent, n: int) {
	for _ in 0 ..< n {
		player.update(p, w, intent, DT)
	}
}


settled_player :: proc(w: ^world.World, pos: [3]f32) -> player.Player {
	p := player.Player {
		position = pos,
		yaw      = math.PI,
	}
	tick(&p, w, {}, 64)
	return p
}
