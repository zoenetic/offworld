package main

import "platform"

SIM_DT :: f32(1.0 / 60.0)
MAX_FRAME_TIME :: f32(0.25)

main :: proc() {
	platform.init_window(1280, 720, "offworld")
	defer platform.close_window()

	accumulator: f32 = 0
	camera := platform.Camera {
		position = {12, 10, 12},
		target   = {0, 0, 0},
		up       = {0, 1, 0},
		fovy     = 45,
	}

	for !platform.should_close() {
		frame := min(platform.frame_time(), MAX_FRAME_TIME)
		accumulator += frame
		for accumulator >= SIM_DT {
			// update the sim by SIM_DT
			accumulator -= SIM_DT
		}

		platform.begin_frame()
		platform.begin_3d(camera)
		platform.draw_grid(20, 1)
		platform.draw_cube({0, 0.5, 0}, {1, 1, 1}, {200, 80, 60, 255})
		platform.end_3d()
		platform.end_frame()
	}
}
