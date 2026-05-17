package dev.zoenetic.offworld.worldgen

data class WeightedBiome(val biome: BiomeId, val weight: Double)

class BiomeContext private constructor(
    val primary: BiomeId,
    val blend: List<WeightedBiome>,
) {
    companion object {
        fun of(primary: BiomeId, blend: List<WeightedBiome>): BiomeContext {
            require(blend.isNotEmpty()) { "blend must not be empty" }
            val sum = blend.sumOf { it.weight }
            require(kotlin.math.abs(sum - 1.0) < 1e-6) {
                "blend weights must sum to 1; was $sum"
            }
            return BiomeContext(primary, blend)
        }
    }
}

/**
 * [resolve] must be a pure, continuous function of [climate] alone.
 */
interface BiomeResolver {
    fun resolve(climate: ClimateSample): BiomeContext
}