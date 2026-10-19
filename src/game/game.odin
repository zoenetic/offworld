package game

import "core:math"

import "../platform"
import "../player"
import "../world"

SIM_DT :: f32(1.0 / 60.0)
MAX_FRAME_TIME :: f32(0.25)
RESPAWN_Y :: f32(-20)

Game :: struct {
	world:        world.World,
	player:       player.Player,
	spawn:        [3]f32,
	chunk_meshes: map[[3]int]platform.Gpu_Mesh,
	accumulator:  f32,
	jump_latch:   bool,
}

init :: proc(g: ^Game) {
	g.world = world.make_world(world.DEFAULT_TERRAIN)
	g.chunk_meshes = make(map[[3]int]platform.Gpu_Mesh)
	g.spawn = {4, world.terrain_height(world.DEFAULT_TERRAIN, 4, 4) + 2, 4}
	g.player = player.Player {
		position = g.spawn,
		yaw      = math.PI,
	}
	stream(g, budget = max(int))
	platform.capture_mouse()
}

destroy :: proc(g: ^Game) {
	for _, &m in g.chunk_meshes do platform.unload_mesh(&m)
	delete(g.chunk_meshes)
	world.destroy_world(&g.world)
}

frame :: proc(g: ^Game) {
	frame_dt := min(platform.frame_time(), MAX_FRAME_TIME)

	player.turn(&g.player, platform.mouse_delta())
	intent := read_intent(g)
	stream(g, LOADS_PER_FRAME)

	g.accumulator += frame_dt
	for g.accumulator >= SIM_DT {
		player.update(&g.player, &g.world, intent, SIM_DT)
		intent.jump = false
		g.jump_latch = false
		if g.player.position.y < RESPAWN_Y {
			g.player.position = g.spawn
			g.player.velocity = {}
		}
		g.accumulator -= SIM_DT
	}

	draw(g)
}

@(private = "file")
draw :: proc(g: ^Game) {
	eye := player.eye(g.player)
	camera := platform.Camera {
		position = eye,
		target   = eye + player.look_direction(g.player),
		up       = {0, 1, 0},
		fovy     = 70,
	}
	platform.begin_frame()
	platform.begin_3d(camera)
	for _, m in g.chunk_meshes do platform.draw_mesh(m)
	platform.end_3d()
	platform.end_frame()
}
