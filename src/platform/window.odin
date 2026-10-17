package platform

import rl "vendor:raylib"

init_window :: proc(width, height: i32, title: cstring) {
	rl.SetConfigFlags({.VSYNC_HINT})
	rl.InitWindow(width, height, title)
}

should_close :: proc() -> bool {
	return rl.WindowShouldClose()
}

close_window :: proc() {
	rl.CloseWindow()
}

frame_time :: proc() -> f32 {
	return rl.GetFrameTime()
}

begin_frame :: proc() {
	rl.BeginDrawing()
	rl.ClearBackground(rl.RAYWHITE)
}

end_frame :: proc() {
	rl.EndDrawing()
}
