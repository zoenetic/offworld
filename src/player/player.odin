package player

import "../world"
import "core:math"
import "core:math/linalg"

SENSITIVITY :: f32(0.003)

RADIUS :: f32(0.5)
EYE_HEIGHT :: f32(1.0)
WALK_SPEED :: f32(6)
JUMP_SPEED :: f32(10)
GRAVITY :: f32(30)
MAX_PITCH :: 1.55
MAX_FALL_SPEED :: f32(25)

Player :: struct {
	position: [3]f32,
	velocity: [3]f32,
	yaw:      f32,
	pitch:    f32,
	grounded: bool,
}

turn :: proc(p: ^Player, mouse: [2]f32) {
	p.yaw -= mouse.x * SENSITIVITY
	p.pitch = clamp(p.pitch - mouse.y * SENSITIVITY, -MAX_PITCH, MAX_PITCH)
}

eye :: proc(p: Player) -> [3]f32 {
	return p.position + {0, EYE_HEIGHT, 0}
}

movement_direction :: proc(intent: Intent, yaw: f32) -> [3]f32 {
	forward := [3]f32{math.sin(yaw), 0, math.cos(yaw)}
	right := linalg.cross(forward, [3]f32{0, 1, 0})
	dir := forward * intent.forward + right * intent.right
	if l := linalg.length(dir); l > 1 do dir /= l
	return dir
}

update :: proc(p: ^Player, w: ^world.World, intent: Intent, dt: f32) {
	direction := movement_direction(intent, p.yaw)
	p.velocity.x = direction.x * WALK_SPEED
	p.velocity.z = direction.z * WALK_SPEED
	if p.grounded && intent.jump do p.velocity.y = JUMP_SPEED
	p.velocity.y = max(p.velocity.y - GRAVITY * dt, -MAX_FALL_SPEED)
	p.position += p.velocity * dt
	resolve_collision(p, w)
}

resolve_collision :: proc(p: ^Player, w: ^world.World) {
	p.grounded = false
	for _ in 0 ..< 3 {
		d := world.sample(w, p.position)
		if d >= RADIUS do break
		n := world.gradient(w, p.position)
		if n == {0, 0, 0} do break
		if n.y > world.WALKABLE_NORMAL_Y {
			p.position.y += (RADIUS - d) / n.y
			if p.velocity.y < 0 do p.velocity.y = 0
			p.grounded = true
		} else {
			p.position += n * (RADIUS - d)
			if into := linalg.dot(p.velocity, n); into < 0 do p.velocity -= n * into
		}
	}
}

look_direction :: proc(p: Player) -> [3]f32 {
	return {
		math.cos(p.pitch) * math.sin(p.yaw),
		math.sin(p.pitch),
		math.cos(p.pitch) * math.cos(p.yaw),
	}
}
