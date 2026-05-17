package dev.zoenetic.offworld.worldgen.fields

import dev.zoenetic.offworld.worldgen.Field
import dev.zoenetic.offworld.worldgen.FieldMeta
import kotlin.math.pow

class FractalField(
    seed: Long,
    private val octaves: Int = 4,
    period: Double = 128.0,
    private val lacunarity: Double = 2.0,
    private val gain: Double = 0.5,
    columnConstant: Boolean = false,
) : Field {

    init {
        require(octaves >= 1) { "octaves must be >= 1; was $octaves" }
        require(lacunarity > 1.0) { "lacunarity must be > 1; was $lacunarity" }
        require(gain > 0.0 && gain < 1.0) { "gain must be in (0,1); was $gain" }
    }

    private val layers = (0 until octaves).map { o ->
        PerlinField(
            seed + o * 0x9E3779B1L,
            period / lacunarity.pow(o.toDouble()),
            columnConstant,
        )
    }

    private val norm: Double = run {
        var amp = 1.0; var sum = 0.0
        repeat(octaves) { sum += amp; amp *= gain }
        if (sum == 0.0) 1.0 else sum
    }

    override val meta = FieldMeta(
        nativeResolution = maxOf(
            1,
            (period / lacunarity.pow((octaves - 1).toDouble()) / 4.0).toInt(),
        ),
        interpolable = true,
        columnConstant = columnConstant,
    )

    override fun sample(x: Double, y: Double, z: Double): Double {
        var amp = 1.0; var acc = 0.0
        for (l in layers) { acc += amp * l.sample(x, y, z); amp *= gain }
        return acc / norm
    }
}