package dev.zoenetic.offworld.worldgen

import dev.zoenetic.offworld.worldgen.fields.FractalField
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test

class BlendViz {

    private val seed = 1234L
    private val size = 768
    private val originX = -384
    private val originZ = -384

    private val space = object : ClimateSpace {
        override val axes = setOf(StandardAxes.TEMPERATURE, StandardAxes.HUMIDITY)
        private val temp = FractalField(seed, period = 256.0, columnConstant = true)
        private val humid = FractalField(seed + 1, period = 256.0, columnConstant = true)
        override fun sampleAt(x: Int, y: Int, z: Int) = ClimateSample.of(
            mapOf(
                StandardAxes.TEMPERATURE to temp.sample(x.toDouble(), 0.0, z.toDouble()),
                StandardAxes.HUMIDITY to humid.sample(x.toDouble(), 0.0, z.toDouble())
            ),
        )
    }

    private fun entry(raw: Int, t: Double, h: Double) =
        ParameterTreeResolver.BiomeEntry(
            biome = BiomeId(raw),
            ideal = mapOf(StandardAxes.TEMPERATURE to t, StandardAxes.HUMIDITY to h),
            ranges = mapOf(
                StandardAxes.TEMPERATURE to (t -0.3)..(t + 0.3),
                StandardAxes.HUMIDITY to (h -0.3)..(h + 0.3),
            ),
        )

    private val entries = listOf(
        entry(0, -0.5, -0.5), entry(1, 0.5, -0.5),
        entry(2, -0.5, 0.5), entry(3, 0.5, 0.5),
    )

    private val palette = mapOf(
        0 to intArrayOf(54, 100, 196),
        1 to intArrayOf(206, 78, 54),
        2 to intArrayOf(64, 168, 96),
        3 to intArrayOf(224, 184, 64),
    )

    @Test
    fun renderBlendMaps() {
        val outDir = File("build/viz").apply { mkdirs() }
        for (sigma in listOf(0.35, 0.5, 0.7, 1.0)) {
            val resolver = ParameterTreeResolver.create(
                entries = entries,
                axes = listOf(StandardAxes.TEMPERATURE, StandardAxes.HUMIDITY),
                k = 4, sigma = sigma,
            )
            val primaryImg = BufferedImage(size, size, BufferedImage.TYPE_INT_RGB)
            val blendImg = BufferedImage(size, size, BufferedImage.TYPE_INT_RGB)

            for (py in 0 until size) {
                val wz = originZ + py
                for (px in 0 until size) {
                    val wx = originX + px
                    val bc = resolver.resolve(space.sampleAt(wx, 0, wz))

                    val pc = palette.getValue(bc.primary.raw)
                    primaryImg.setRGB(px, py, rgb(pc[0], pc[1], pc[2]))

                    var r = 0.0; var g = 0.0; var b = 0.0
                    for (wb in bc.blend) {
                        val c = palette.getValue(wb.biome.raw)
                        r += wb.weight * c[0]
                        g += wb.weight * c[1]
                        b += wb.weight * c[2]
                    }
                    blendImg.setRGB(px, py, rgb(r.toInt(), g.toInt(), b.toInt()))
                }
            }
            val tag = "sigma_${sigma.toString().replace('.', '_')}"
            ImageIO.write(primaryImg, "png", File(outDir, "primary_$tag.png"))
            ImageIO.write(blendImg, "png", File(outDir, "blend_$tag.png"))
        }
        println("wrote ${outDir.absolutePath}/{primary,blend}_sigma_*.png")
    }

    private fun rgb(r: Int, g: Int, b: Int): Int {
        fun c(v: Int) = v.coerceIn(0, 255)
        return (c(r) shl 16) or (c(g) shl 8) or c(b)
    }
}