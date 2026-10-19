package game

import "../mesh"
import "../platform"
import "../world"

LOADS_PER_FRAME :: 2

stream :: proc(g: ^Game, budget: int) {
	center, _ := world.locate(g.player.position)
	center.y = 0
	if center not_in g.world.chunks do load(g, center)

	plan := world.plan_streaming(&g.world, center)
	defer world.delete_plan(&plan)
	for coord in plan.load[:min(budget, len(plan.load))] do load(g, coord)
	for coord in plan.unload do unload(g, coord)
}

@(private = "file")
load :: proc(g: ^Game, coord: [3]int) {
	chunk := world.load_chunk(&g.world, coord)
	m := mesh.build(chunk)
	defer mesh.destroy(&m)
	if len(m.positions) > 0 do g.chunk_meshes[coord] = platform.upload_mesh(m.positions[:], m.colours[:])
}

@(private = "file")
unload :: proc(g: ^Game, coord: [3]int) {
	if m, ok := g.chunk_meshes[coord]; ok {
		platform.unload_mesh(&m)
		delete_key(&g.chunk_meshes, coord)
	}
	world.unload_chunk(&g.world, coord)
}
