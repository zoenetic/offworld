package main

import "game"
import "platform"

main :: proc() {
	platform.init_window(1920, 1080, "offworld")
	defer platform.close_window()
	g: game.Game
	game.init(&g)
	defer game.destroy(&g)
	for !platform.should_close() do game.frame(&g)
}
