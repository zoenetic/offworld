package platform

import rl "vendor:raylib"

Camera :: struct {
	position: [3]f32,
	target:   [3]f32,
	up:       [3]f32,
	fovy:     f32,
}

begin_3d :: proc(camera: Camera) {
	rl.BeginMode3D(
		rl.Camera3D {
			position = camera.position,
			target = camera.target,
			up = camera.up,
			fovy = camera.fovy,
			projection = .PERSPECTIVE,
		},
	)
}

end_3d :: proc() {
	rl.EndMode3D()
}

draw_grid :: proc(slices: i32, spacing: f32) {
	rl.DrawGrid(slices, spacing)
}

draw_cube :: proc(center, size: [3]f32, colour: [4]u8) {
	rl.DrawCubeV(center, size, rl.Color(colour))
}
