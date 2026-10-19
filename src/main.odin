package main

import "core:math"

import "mesh"
import "platform"
import "player"
import "world"

SIM_DT :: f32(1.0 / 60.0)
MAX_FRAME_TIME :: f32(0.25)

key_axis :: proc(negative, positive: platform.Key) -> f32 {
	v: f32
	if platform.key_down(positive) do v += 1
	if platform.key_down(negative) do v -= 1
	return v
}

main :: proc() {
	platform.init_window(1920, 1080, "offworld")
	defer platform.close_window()

	chunk: world.Chunk
	world.fill_terrain(&chunk, world.DEFAULT_TERRAIN, {0, 0, 0})

	terrain := mesh.build(&chunk)
	defer mesh.destroy(&terrain)

	terrain_gpu := platform.upload_mesh(terrain.positions[:], terrain.colours[:])
	defer platform.unload_mesh(&terrain_gpu)

	spawn := world.terrain_height(world.DEFAULT_TERRAIN, 4, 4) + 2

	p := player.Player {
		position = spawn,
		yaw      = math.PI,
	}

	accumulator: f32 = 0

	jump_latch := false

	platform.capture_mouse()

	for !platform.should_close() {
		frame_dt := min(platform.frame_time(), MAX_FRAME_TIME)
		mouse := platform.mouse_delta()
		p.yaw -= mouse.x * player.SENSITIVITY
		p.pitch -= mouse.y * player.SENSITIVITY
		p.pitch = clamp(p.pitch, -player.MAX_PITCH, player.MAX_PITCH)

		if platform.key_pressed(.SPACE) do jump_latch = true

		intent := player.Intent {
			forward = key_axis(.S, .W),
			right   = key_axis(.A, .D),
			jump    = jump_latch,
		}

		accumulator += frame_dt

		for accumulator >= SIM_DT {
			player.update(&p, &chunk, intent, SIM_DT)
			intent.jump = false
			jump_latch = false
			if p.position.y < -20 {
				p.position = spawn
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
		platform.draw_mesh(terrain_gpu)
		platform.end_3d()
		platform.end_frame()
	}
}
