package tests

import "../src/player"
import "../src/world"
import "core:math"

DT :: f32(1.0 / 60.0)

approx :: proc(a, b: f32, eps := f32(1e-3)) -> bool {
	return math.abs(a - b) <= eps
}

new_test_world :: proc() -> ^world.Chunk {
	chunk := new(world.Chunk)
	world.fill_test_scene(chunk)
	return chunk
}

tick :: proc(p: ^player.Player, chunk: ^world.Chunk, intent: player.Intent, n: int) {
	for _ in 0 ..< n {
		player.update(p, chunk, intent, DT)
	}
}


settled_player :: proc(chunk: ^world.Chunk, pos: [3]f32) -> player.Player {
	p := player.Player {
		position = pos,
		yaw      = math.PI,
	}
	tick(&p, chunk, {}, 64)
	return p
}
