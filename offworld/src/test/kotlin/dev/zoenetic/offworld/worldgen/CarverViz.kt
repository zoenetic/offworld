package dev.zoenetic.offworld.worldgen

import org.junit.jupiter.api.Test
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

class CarverViz {

    private val world = WorldContext(seed = 1234L, minY = -64, height = 384)

    private val shaper = DefaultTerrainShaper(
        skeleton = object : DensityNode {
            override val validContexts = SamplingContext.entries.toSet()
            override fun eval(ctx: EvalCtx): Double {
                val surface = 60.0 + 0.02 * ctx.x
                return surface - ctx.y
            }
        },
        profileOf = {
            object : TerrainProfile { override fun modulation(ctx: EvalCtx) = 0.0 }
        },
    )

    @Test
    fun renderCaveCrossSections() {
        val outDir = File("build/viz").apply { mkdirs() }
        val carver = TunnelCarver()
        val chunkRange = -2..2
        val masks = HashMap<ChunkPos, CarveMask>()
        for (cz in chunkRange) for (cx in chunkRange) {
            val pos = ChunkPos(cx, cz)
            val m = CarveMask(world.minY, world.height)
            carver.carve(pos, world, shaper, m)
            masks[pos] = m
        }
        val xMin = chunkRange.first * 16
        val xMax = chunkRange.last + 16 + 15
        val yMin = world.minY
        val yMax = 80
        val w = xMax - xMin + 1
        val h = yMax - yMin + 1
        for (worldZ in intArrayOf(-20, 0, 8, 24)) {
            val img = BufferedImage(w, h, BufferedImage.TYPE_INT_RGB)
            for (py in 0 until h) {
                val wy = yMax - py
                for (px in 0 until w) {
                    val wx = xMin + px
                    val cpos = ChunkPos(Math.floorDiv(wx, 16), Math.floorDiv(worldZ, 16))
                    val lx = Math.floorMod(wx, 16)
                    val lz = Math.floorMod(worldZ, 16)
                    val carved = masks[cpos]?.isCarved(lx, lz, wy) ?: false
                    val solid = shaper.density(
                        EvalCtx(wx.toDouble(), wy.toDouble(), worldZ.toDouble(),
                            world, biome = null),
                    ) > 0.0
                    val color = when {
                        carved -> 0xE08A2A
                        solid  -> 0x4A4A4A
                        else   -> 0x000000
                    }
                    img.setRGB(px, py, color)
                }
            }
            ImageIO.write(img, "png", File(outDir, "cave_z${worldZ}.png"))
        }
        println("wrote ${outDir.absolutePath}/cave_z*.png")
    }
}