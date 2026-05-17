package dev.zoenetic.offworld.worldgen

import kotlin.math.exp

class ParameterTreeResolver private constructor(
    private val entries: List<BiomeEntry>,
    private val axes: List<ClimateAxis>,
    private val k: Int,
    private val sigma: Double,
) : BiomeResolver {

    class BiomeEntry(
        val biome: BiomeId,
        val ideal: Map<ClimateAxis, Double>,
        val ranges: Map<ClimateAxis, ClosedFloatingPointRange<Double>>,
    )

    private fun BiomeEntry.distanceSq(c: ClimateSample): Double {
        var d = 0.0
        for (axis in axes) {
            val v = c[axis]
            val r = ranges[axis]
            val toIdeal = v - (ideal[axis] ?: (r?.start ?: 0.0))
            val over = when {
                r == null -> 0.0
                v < r.start -> r.start - v
                v > r.endInclusive -> v - r.endInclusive
                else -> 0.0
            }
            val inBox = r == null || v in r
            d += if (inBox) toIdeal * toIdeal
                else toIdeal * toIdeal + 4.0 * over * over
        }
        return d
    }

    override fun resolve(climate: ClimateSample): BiomeContext {
        val ranked = entries
            .map { it to it.distanceSq(climate) }
            .sortedBy { it.second }
        val nearest = ranked.subList(0, minOf(k, ranked.size))
        val dMin = nearest.first().second
        val raw = nearest.map { (e, dSq) ->
            e.biome to exp(-(dSq - dMin) / (sigma * sigma))
        }
        val sum = raw.sumOf { it.second }
        val blend = raw.map { (b, w) -> WeightedBiome(b, w / sum) }
        return BiomeContext.of(primary = blend.first().biome, blend = blend)
    }

    companion object {
        const val DEFAULT_SIGMA = 0.7
        const val DEFAULT_K = 4

        fun create(
            entries: List<BiomeEntry>,
            axes: List<ClimateAxis>,
            k: Int = DEFAULT_K,
            sigma: Double = DEFAULT_SIGMA,
        ): ParameterTreeResolver {
            require(entries.isNotEmpty()) { "need at least one biome entry" }
            require(k in 1..8) { "k is out of sane range; was $k"}
            require(sigma > 0.0) { "sigma must be positive; was $sigma" }
            return ParameterTreeResolver(entries, axes, k, sigma)
        }
    }
}