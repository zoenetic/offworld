package dev.zoenetic.offworld.worldgen

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class CarveMask(
    private val minY: Int,
    private val height: Int,
) {
    init { require(height > 0) { "height must be positive; was $height" } }

    private val bits = java.util.BitSet(16 * 16 * height)

    private fun idx(lx: Int, lz: Int, y: Int): Int {
        require(lx in 0..15 && lz in 0..15) { "column out of chunk: ($lx,$lz)" }
        val ly = y - minY
        require(ly in 0 until height) { "y out of range: $y" }
        return (lx * 16 + lz) * height + ly
    }

    fun carve(lx: Int, lz: Int, y: Int) = bits.set(idx(lx, lz, y))

    fun isCarved(lx: Int, lz: Int, y: Int): Boolean {
        val ly = y - minY
        if (lx !in 0..15 || lz !in 0..15 || ly !in 0 until height) return false
        return bits.get(idx(lx, lz, y))
    }
}

interface Carver {
    val id: String
    val provides: Set<String>
    val requires: Set<String>
    fun carve(
        pos: ChunkPos,
        world: WorldContext,
        density: TerrainShaper,
        mask: CarveMask,
    )
}

class TunnelCarver(
    override val id: String = "offworld:caves",
    override val provides: Set<String> = setOf("caves_carved"),
    override val requires: Set<String> = emptySet(),
    private val tunnelsPerChunk: Double = 0.7,
    private val maxLength: Int = 112,
    private val radius: Double = 2.6,
    private val originSearchRadius: Int = 7,
    private val branchChance: Double = 0.10,
    private val cavernChance: Double = 0.012,
    private val largeCavernChance: Double = 0.05,
) : Carver {

    init {
        require(maxLength <= originSearchRadius * 16) {
            "maxLength ($maxLength) exceeds reach of originSearchRadius " +
                    "($originSearchRadius chunks = ${originSearchRadius * 16} blocks); " +
                    "tunnels would clip at chunk borders"
        }
        require(tunnelsPerChunk in 0.0..1.0) {
            "tunnelsPerChunk must be in [0,1]; was $tunnelsPerChunk"
        }
        require(branchChance in 0.0..1.0) { "branchChance must be in [0,1]" }
        require(cavernChance in 0.0..1.0) { "cavernChance must be in [0,1]" }
        require(largeCavernChance in 0.0..1.0) { "largeCavernChance must be in [0,1]" }
    }

    override fun carve(
        pos: ChunkPos,
        world: WorldContext,
        density: TerrainShaper,
        mask: CarveMask,
    ) {
        for (cz in pos.z - originSearchRadius..pos.z + originSearchRadius) {
            for (cx in pos.x - originSearchRadius..pos.x + originSearchRadius) {
                val originRng = chunkRng(world.seed, cx, cz)
                if (originRng.nextDouble() < tunnelsPerChunk) {
                    val x = (cx shl 4) + originRng.nextInt(16) + 0.5
                    val z = (cz shl 4) + originRng.nextInt(16) + 0.5
                    val y = world.minY + 8 +
                            originRng.nextInt(maxOf(1, world.height / 2)).toDouble()
                    val yaw = originRng.nextDouble() * 2.0 * PI
                    val pitch = (originRng.nextDouble() - 0.5) * 0.5
                    val len = maxLength / 2 + originRng.nextInt(maxLength / 2)
                    carveTunnel(
                        x, y, z, yaw, pitch, len,
                        originRng.nextDouble() * 2.0 * PI,
                        originRng, pos, world, density, mask, depth = 0,
                    )
                }
                if (originRng.nextDouble() < largeCavernChance) {
                    val x = (cx shl 4) + originRng.nextInt(16) + 0.5
                    val z = (cz shl 4) + originRng.nextInt(16) + 0.5
                    val y = world.minY + 6 +
                            originRng.nextInt(maxOf(1, world.height / 3)).toDouble()
                    carveCavern(x, y, z, originRng, pos, world, density, mask)
                }
            }
        }
    }

    private fun carveTunnel(
        startX: Double, startY: Double, startZ: Double,
        startYaw: Double, startPitch: Double,
        len: Int,
        meanderPhase: Double,
        rng: PositionalRandom,
        target: ChunkPos,
        world: WorldContext,
        density: TerrainShaper,
        mask: CarveMask,
        depth: Int,
    ) {
        var x = startX
        var y = startY
        var z = startZ
        var yaw = startYaw
        var pitch = startPitch
        for (step in 0 until len) {
            val meander = sin(meanderPhase + step * 0.06) * 0.18
            yaw += (rng.nextDouble() - 0.5) * 0.55 + meander
            pitch = (pitch + (rng.nextDouble() - 0.5) * 0.32 +
                    sin(meanderPhase * 1.7 + step * 0.045) * 0.06).coerceIn(-0.85, 0.85)
            x += cos(yaw) * cos(pitch)
            z += sin(yaw) * cos(pitch)
            y += sin(pitch)
            val along = step.toDouble() / len
            val r = radius * (0.6 + 0.5 * sin(along * PI) +
                    0.35 * sin(step * 0.31) + 0.2 * sin(step * 0.11))
            stamp(x, y, z, r.coerceAtLeast(1.0), 1.0, 1.0, target, world, density, mask)
            if (rng.nextDouble() < cavernChance) {
                stamp(
                    x, y, z, radius * 2.2,
                    1.0 + rng.nextDouble() * 0.8,
                    0.55 + rng.nextDouble() * 0.3,
                    target, world, density, mask,
                )
            }
            if (depth < 2 && rng.nextDouble() < branchChance) {
                carveTunnel(
                    x, y, z,
                    yaw + (rng.nextDouble() - 0.5) * 1.8,
                    (pitch + (rng.nextDouble() - 0.5) * 0.5).coerceIn(-0.85, 0.85),
                    (len - step) / 2 + 1,
                    rng.nextDouble() * 2.0 * PI,
                    rng, target, world, density, mask,
                    depth = depth + 1,
                )
            }
        }
    }

    private fun carveCavern(
        cx: Double, cy: Double, cz: Double,
        rng: PositionalRandom,
        target: ChunkPos,
        world: WorldContext,
        density: TerrainShaper,
        mask: CarveMask,
    ) {
        val blobs = 4 + rng.nextInt(5)
        var x = cx
        var y = cy
        var z = cz
        for (i in 0 until blobs) {
            x += (rng.nextDouble() - 0.5) * radius * 3.0
            y += (rng.nextDouble() - 0.5) * radius * 1.4
            z += (rng.nextDouble() - 0.5) * radius * 3.0
            val r = radius * (2.8 + rng.nextDouble() * 2.4)
            stamp(
                x, y, z, r,
                0.85 + rng.nextDouble() * 0.6,
                0.5 + rng.nextDouble() * 0.25,
                target, world, density, mask,
            )
        }
    }

    private fun stamp(
        wx: Double, wy: Double, wz: Double, r: Double,
        scaleXZ: Double, scaleY: Double,
        target: ChunkPos, world: WorldContext,
        density: TerrainShaper, mask: CarveMask,
    ) {
        val rx = r * scaleXZ
        val ry = r * scaleY
        val rxi = rx.toInt()
        val ryi = ry.toInt()
        val minX = target.minBlockX
        val minZ = target.minBlockZ
        for (dx in -rxi..rxi)
            for (dy in -ryi..ryi)
                for (dz in -rxi..rxi) {
                    val nx = dx / rx
                    val ny = dy / ry
                    val nz = dz / rx
                    if (nx * nx + ny * ny + nz * nz > 1.0) continue
                    val bx = (wx + dx).toInt()
                    val by = (wy + dy).toInt()
                    val bz = (wz + dz).toInt()
                    val lx = bx - minX
                    val lz = bz - minZ
                    if (lx !in 0..15 || lz !in 0..15) continue
                    if (by !in world.minY until world.maxY) continue
                    val solid = density.density(
                        EvalCtx(bx.toDouble(), by.toDouble(), bz.toDouble(),
                            world, biome = null),
                    ) > 0.0
                    if (solid) mask.carve(lx, lz, by)
                }
    }

    private fun chunkRng(seed: Long, cx: Int, cz: Int): PositionalRandom {
        var s = seed xor (cx.toLong() * 0x9E3779B97F4A7C15uL.toLong())
        s = s xor (cz.toLong() * 0xC2B2AE3D27D4EB4FuL.toLong())
        return object : PositionalRandom {
            override fun nextInt(bound: Int): Int {
                require(bound > 0) { "bound must be positive" }
                s = s * 6364136223846793005L + 1442695040888963407L
                return ((s ushr 33).toInt() and 0x7fffffff) % bound
            }
            override fun nextDouble(): Double {
                s = s * 6364136223846793005L + 1442695040888963407L
                return ((s ushr 11) and 0x1FFFFFFFFFFFFFL).toDouble() / (1L shl 53)
            }
        }
    }
}