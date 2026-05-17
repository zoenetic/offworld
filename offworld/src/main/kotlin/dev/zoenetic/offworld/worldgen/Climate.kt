package dev.zoenetic.offworld.worldgen

@JvmInline
value class ClimateAxis(val name: String)

object StandardAxes {
    val TEMPERATURE = ClimateAxis("temperature")
    val HUMIDITY = ClimateAxis("humidity")
    val CONTINENTALNESS = ClimateAxis("continentalness")
    val EROSION = ClimateAxis("erosion")
    val WEIRDNESS = ClimateAxis("weirdness")
    val DEPTH = ClimateAxis("depth")
}

class ClimateSample private constructor(
    private val values: Map<ClimateAxis, Double>
) {
    operator fun get(axis: ClimateAxis): Double =
        values[axis]
            ?: error(
                "Climate axis '${axis.name}' not present in this sample; " +
                    "sample provides ${values.keys.map { it.name }}",
            )

    fun axes(): Set<ClimateAxis> = values.keys

    companion object {
        fun of(values: Map<ClimateAxis, Double>): ClimateSample {
            require(values.isNotEmpty()) {
                "ClimateSample must have at least one axis"
            }
            return ClimateSample(values.toMap())
        }
    }
}

/**
 * Every axis a [ClimateSpace] provides must be spatially continuous
 */
interface ClimateSpace {
    val axes: Set<ClimateAxis>
    fun sampleAt(x: Int, y: Int, z: Int): ClimateSample
}