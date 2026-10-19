package world

import "core:math/linalg"
import "core:slice"

LOAD_RADIUS :: 3
UNLOAD_RADIUS :: LOAD_RADIUS + 1

Plan :: struct {
	load:   [dynamic][3]int,
	unload: [dynamic][3]int,
}

@(private)
Candidate :: struct {
	coord: [3]int,
	dist2: int,
}

delete_plan :: proc(p: ^Plan) {
	delete(p.load)
	delete(p.unload)
}

plan_streaming :: proc(w: ^World, center: [3]int) -> (p: Plan) {
	for dz in -LOAD_RADIUS ..= LOAD_RADIUS {
		for dx in -LOAD_RADIUS ..= LOAD_RADIUS {
			offset := [3]int{dx, 0, dz}
			if center + offset not_in w.chunks do append(&p.load, offset)
		}
	}
	slice.sort_by(
		p.load[:],
		proc(a, b: [3]int) -> bool {return linalg.length2(a.xz) < linalg.length2(b.xz)},
	)
	for &c in p.load do c += center
	for coord in w.chunks {
		if ring(coord - center) > UNLOAD_RADIUS do append(&p.unload, coord)
	}
	return
}

@(private)
by_distance :: proc(a, b: Candidate) -> bool {return a.dist2 < b.dist2}

@(private)
ring :: proc(offset: [3]int) -> int {
	return max(abs(offset.x), abs(offset.z))
}
