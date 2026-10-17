package main

import "core:math"

import "platform"
import "player"
import "world"

SIM_DT :: f32(1.0 / 60.0)
MAX_FRAME_TIME :: f32(0.25)
SPAWN :: [3]f32{4, 8, 4}

key_axis :: proc(negative, positive: platform.Key) -> f32 {
	v: f32
	if platform.key_down(positive) do v += 1
	if platform.key_down(negative) do v -= 1
	return v
}

main :: proc() {
	platform.init_window(1280, 720, "offworld")
	defer platform.close_window()

	chunk: world.Chunk
	world.fill_test_scene(&chunk)
	p := player.Player {
		position = SPAWN,
		yaw      = math.PI,
	}

	accumulator: f32 = 0

	jump_latch := false

	platform.capture_mouse()

	for !platform.should_close() {
		frame := min(platform.frame_time(), MAX_FRAME_TIME)
		delta := platform.mouse_delta()
		p.yaw -= delta.x * player.SENSITIVITY
		p.pitch -= delta.y * player.SENSITIVITY
		p.pitch = clamp(p.pitch, -1.55, 1.55)

		if platform.key_pressed(.Jump) do jump_latch = true

		intent := player.Intent {
			forward = key_axis(.S, .W),
			right   = key_axis(.A, .D),
			jump    = jump_latch,
		}

		accumulator += frame

		for accumulator >= SIM_DT {
			player.update(&p, &chunk, intent, SIM_DT)
			intent.jump = false
			jump_latch = false
			if p.position.y < -20 {
				p.position = SPAWN
				p.velocity = {0, 0, 0}
			}
			accumulator -= SIM_DT
		}

		eye := p.position + {0, player.EYE_HEIGHT, 0}
		camera := platform.Camera {
			position = eye,
			target   = eye + player.look_direction(p),
			up       = {0, 1, 0},
			fovy     = 70,
		}

		platform.begin_frame()
		platform.begin_3d(camera)
		for z in 0 ..< world.CHUNK_SIZE {
			for y in 0 ..< world.CHUNK_SIZE {
				for x in 0 ..< world.CHUNK_SIZE {
					if chunk.density[world.index(x, y, z)] < 0 {
						platform.draw_cube(
							{f32(x), f32(y), f32(z)},
							{0.9, 0.9, 0.9},
							{120, 160, 90, 255},
						)
					}
				}
			}
		}
		platform.end_3d()
		platform.end_frame()
	}
}
