package dev.zoenetic.offworld.tools.texturegen

import dev.zoenetic.offworld.block.BlockKind
import dev.zoenetic.offworld.block.OffworldBlocks
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

fun main(args: Array<String>) {
    require(args.size == 1) { "must pass an output directory"}
    val outDir = File(args[0])
    for (def in OffworldBlocks.defs) {
        val path = def.key.path
        val colour = Color.decode(def.hex)
        val texDir = File(outDir, "assets/offworld/textures/block").also { it.mkdirs() }
        val img = BufferedImage(16, 16, BufferedImage.TYPE_INT_RGB)
        ImageIO.write(img, "PNG", File(texDir, "$path.png"))
        if (def.kind == BlockKind.CUBE_ALL) {
            File(outDir, "assets/offworld/blockstates").also { it.mkdirs() }
                .resolve("$path.json")
                .writeText("""{"variants":{"":{"model":"offworld:block/$path"}}}""")
            File(outDir, "assets/offworld/models/block").also { it.mkdirs() }
                .resolve("$path.json")
                .writeText("""{"parent":"minecraft:block/cube_all","textures":{"all":"offworld:block/$path"}}""")
            File(outDir, "assets/offworld/items").also { it.mkdirs() }
                .resolve("$path.json")
                .writeText("""{"model":{"type":"minecraft:model","model":"offworld:block/$path"}}""")
        }
    }
}