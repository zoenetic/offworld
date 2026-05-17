package dev.zoenetic.offworld.worldgen

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CarverTest {

    @Test
    fun carveMaskRoundTrips() {
        val mask = CarveMask(minY = -64, height = 384)
        mask.carve(3, 5, 10)
        assertTrue(mask.isCarved(3, 5, 10))
        assertFalse(mask.isCarved(5, 3, 10), "column (lx,lz) must be order-sensitive")
        assertFalse(mask.isCarved(3, 5, 11))
    }

    @Test
    fun carveMaskOutOfRangeIsFalseNotThrow() {
        val mask = CarveMask(minY = 0, height = 64)
        assertFalse(mask.isCarved(0, 0, -1))
        assertFalse(mask.isCarved(0, 0, 64))
        assertFalse(mask.isCarved(-1, 0, 0))
        assertFalse(mask.isCarved(16, 0, 0))
    }

    @Test
    fun carveMaskColumnsAreIndependent() {
        val mask = CarveMask(minY = 0, height = 64)
        mask.carve(2, 7, 30)
        for (lx in 0..15) for (lz in 0..15) {
            val expected = lx == 2 && lz == 7
            assertEquals(expected, mask.isCarved(lx, lz, 30),
                "($lx,$lz) carved=${mask.isCarved(lx, lz, 30)} expected=$expected")
        }
    }

    @Test
    fun tunnelCarverRejectsInconsistentReach() {
        assertFailsWith<IllegalArgumentException> {
            TunnelCarver(maxLength = 112, originSearchRadius = 4)
        }
    }

    @Test
    fun tunnelCarverDefaultsAreConsistent() {
        TunnelCarver()
    }

    @Test
    fun tunnelCarverRejectsBadProbability() {
        assertFailsWith<IllegalArgumentException> {
            TunnelCarver(tunnelsPerChunk = 1.5)
        }
    }

    @Test
    fun carveIsDeterministicPerSeed() {
        val world = WorldContext(seed = 42L, minY = -64, height = 384)
        val shaper = DefaultTerrainShaper(
            skeleton = object : DensityNode {
                override val validContexts = SamplingContext.entries.toSet()
                override fun eval(ctx: EvalCtx) = 64.0 - ctx.y
            },
            profileOf = { object : TerrainProfile { override fun modulation(ctx: EvalCtx) = 0.0 } }
        )
        val pos = ChunkPos(0, 0)
        fun run(): CarveMask {
            val m = CarveMask(world.minY, world.height)
            TunnelCarver().carve(pos, world, shaper, m)
            return m
        }
        val a = run(); val b = run()
        for (lx in 0..15) for (lz in 0..15) for (y in world.minY until world.maxY) {
            assertEquals(a.isCarved(lx, lz, y), b.isCarved(lx, lz, y),
                "nondeterministic at ($lx,$lz,$y)")
        }
    }
}