package dev.zoenetic.offworld.worldgen

import kotlin.test.Test
import dev.zoenetic.offworld.worldgen.fields.FractalField
import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class WorldgenTest {
    private val seed = 1234L

    private val space = object : ClimateSpace {
        override val axes = setOf(
            StandardAxes.TEMPERATURE, StandardAxes.HUMIDITY,
        )
        private val temp = FractalField(seed, period = 256.0, columnConstant = true)
        private val humid = FractalField(seed + 1, period = 256.0, columnConstant = true)
        override fun sampleAt(x: Int, y: Int, z: Int) = ClimateSample.of(
            mapOf(
                StandardAxes.TEMPERATURE to temp.sample(x.toDouble(), 0.0, z.toDouble()),
                StandardAxes.HUMIDITY to humid.sample(x.toDouble(), 0.0, z.toDouble()),
            ),
        )
    }

    private fun entry(raw: Int, t: Double, h: Double) =
        ParameterTreeResolver.BiomeEntry(
            biome = BiomeId(raw),
            ideal = mapOf(StandardAxes.TEMPERATURE to t, StandardAxes.HUMIDITY to h),
            ranges = mapOf(
                StandardAxes.TEMPERATURE to (t - 0.3)..(t + 0.3),
                StandardAxes.HUMIDITY to (h - 0.3)..(h + 0.3),
            ),
        )

    private val resolver = ParameterTreeResolver.create(
        entries = listOf(
            entry(0, -0.5, -0.5), entry(1, 0.5, -0.5),
            entry(2, -0.5, 0.5), entry(3, 0.5, 0.5),
        ),
        axes = listOf(StandardAxes.TEMPERATURE, StandardAxes.HUMIDITY),
        k = 4,
        sigma = 0.7,
    )

    @Test
    fun blendWeightsAreSpatiallyContinuous() {
        var prev: Map<Int, Double>? = null
        var maxDelta = 0.0
        for (x in 0..4000) {
            val bc = resolver.resolve(space.sampleAt(x, 0, 0))
            val w = bc.blend.associate { it.biome.raw to it.weight }
            if (prev != null) {
                val keys = prev.keys + w.keys
                for (k in keys) {
                    val d = abs((w[k] ?: 0.0) - (prev[k] ?: 0.0))
                    if (d > maxDelta) maxDelta = d
                }
            }
            prev = w
        }
        assertTrue(maxDelta < 0.03, "max per-block weight delta $maxDelta")
    }

    @Test
    fun resolveIsPureAndDeterministic() {
        val s = space.sampleAt(173, 0, -914)
        val a = resolver.resolve(s)
        val b = resolver.resolve(s)
        assertEquals(a.primary.raw, b.primary.raw)
        assertEquals(a.blend.map { it.biome.raw to it.weight },
            b.blend.map { it.biome.raw to it.weight })
    }

    @Test
    fun blendWeightsSumToOne() {
        for (x in intArrayOf(-5000, -13, 0, 47, 99999)) {
            val bc = resolver.resolve(space.sampleAt(x, 0, x /2))
            assertEquals(1.0, bc.blend.sumOf { it.weight }, 1e-9)
        }
    }

    @Test
    fun primaryIsHighestWeight() {
        val bc = resolver.resolve(space.sampleAt(311, 0, -77))
        val top = bc.blend.maxBy { it.weight }
        assertEquals(top.biome.raw, bc.primary.raw)
    }

    @Test
    fun cellInterpolatedTracksPerBlockWithinBound() {
        val field = FractalField(seed, period = 128.0)
        val region = Region(0, 0, 0, 32, 32, 32)
        val exact = PerBlockSampler().realize(field, region)
        val interp = CellInterpolatedSampler(4, 8).realize(field, region)
        var maxErr = 0.0
        for (y in 0 until 32) for (z in 0 until 32) for (x in 0 until 32) {
            val e = abs(exact[x, y, z] - interp[x, y, z])
            if (e > maxErr) maxErr = e
        }
        assertTrue(maxErr < 0.15, "interpolation error $maxErr")
    }

    @Test
    fun columnSamplerRejectsNonColumnField() {
        val field = FractalField(seed, period = 64.0, columnConstant = false)
        assertFailsWith<IllegalArgumentException> {
            ColumnSampler().realize(field, Region(0, 0, 0, 4, 4, 4))
        }
    }

    @Test
    fun densityCheckedRejectsIncompatibleContextAtBuild() {
        val perBlockOnly = object : DensityNode {
            override val validContexts = setOf(SamplingContext.PER_BLOCK)
            override fun eval(ctx: EvalCtx) = 0.0
        }
        assertFailsWith<DensityGraphError> {
            Density.checked(perBlockOnly, SamplingContext.INTERPOLATED, "$.root.child")
        }
    }

    private fun feat(id: String, prov: Set<String>, req: Set<String>) =
        object : PlacedFeature {
            override val id = id
            override val provides = prov
            override val requires = req
            override fun place(ctx: FeatureCtx) {}
        }

    @Test
    fun schedulerOrdersByCapabilityDependency() {
        val a = feat("a", setOf("ore_pass"), emptySet())
        val b = feat("b", emptySet(), setOf("ore_pass"))
        val ordered = TopoFeatureScheduler().order(listOf(b, a))
        assertTrue(ordered.indexOf(a) < ordered.indexOf(b))
    }

    @Test
    fun schedulerThrowsOnCapabilityCycle() {
        val a = feat("a", setOf("x"), setOf("y"))
        val b = feat("b", setOf("y"), setOf("x"))
        assertFailsWith<FeatureGraphError> {
            TopoFeatureScheduler().order(listOf(a, b))
        }
    }
}