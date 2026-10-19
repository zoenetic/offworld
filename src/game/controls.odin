package game

import "../platform"
import "../player"

read_intent :: proc(g: ^Game) -> player.Intent {
	if platform.key_pressed(.SPACE) do g.jump_latch = true
	return {forward = key_axis(.S, .W), right = key_axis(.A, .D), jump = g.jump_latch}
}

@(private = "file")
key_axis :: proc(negative, positive: platform.Key) -> f32 {
	v: f32
	if platform.key_down(positive) do v += 1
	if platform.key_down(negative) do v -= 1
	return v
}
