package platform

import rl "vendor:raylib"

Key :: rl.KeyboardKey

key_down :: proc(key: Key) -> bool {
	return rl.IsKeyDown(key)
}

key_pressed :: proc(key: Key) -> bool {
	return rl.IsKeyPressed(key)
}


@(private = "file")
skip_next_delta := false

capture_mouse :: proc() {
	rl.DisableCursor()
	skip_next_delta = true
}

mouse_delta :: proc() -> [2]f32 {
	if skip_next_delta {
		skip_next_delta = false
		return {0, 0}
	}
	return rl.GetMouseDelta()
}
