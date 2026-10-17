package platform

import rl "vendor:raylib"

Key :: enum {
	W,
	A,
	S,
	D,
	Jump,
	Crouch,
}

KEY_MAP := [Key]rl.KeyboardKey {
	.W      = .W,
	.A      = .A,
	.S      = .S,
	.D      = .D,
	.Jump   = .SPACE,
	.Crouch = .LEFT_SHIFT,
}

key_down :: proc(key: Key) -> bool {
	return rl.IsKeyDown(KEY_MAP[key])
}

key_pressed :: proc(key: Key) -> bool {
	return rl.IsKeyPressed(KEY_MAP[key])
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
