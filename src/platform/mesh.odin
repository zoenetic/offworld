package platform

import rl "vendor:raylib"

Gpu_Mesh :: struct {
	mesh: rl.Mesh,
}

@(private)
default_material: rl.Material

upload_mesh :: proc(positions: [][3]f32, colours: [][4]u8) -> (g: Gpu_Mesh) {
	assert(len(positions) == len(colours))
	g = {
		mesh = rl.Mesh {
			vertexCount = i32(len(positions)),
			triangleCount = i32(len(positions) / 3),
			vertices = cast([^]f32)raw_data(positions),
			colors = cast([^]u8)raw_data(colours),
		},
	}
	rl.UploadMesh(&g.mesh, false)
	g.mesh.vertices = nil
	g.mesh.colors = nil
	return g
}

draw_mesh :: proc(g: Gpu_Mesh) {
	rl.DrawMesh(g.mesh, default_material, rl.Matrix(1))
}

unload_mesh :: proc(g: ^Gpu_Mesh) {
	rl.UnloadMesh(g.mesh)
	g^ = {}
}
