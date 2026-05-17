package dev.zoenetic.offworld.fabric.worldgen

import dev.zoenetic.offworld.worldgen.WorldgenSpec
import net.minecraft.core.HolderGetter
import net.minecraft.resources.ResourceKey
import net.minecraft.world.level.biome.Biome
import net.minecraft.world.level.biome.Biomes
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState

object OffworldWorldgen {

    fun assemble(
        specFactory: (Long) -> WorldgenSpec,
        biomeLookup: HolderGetter<Biome>,
    ): OffworldChunkGenerator {
        val initialSpec = specFactory(0L)
        val tables = SeamTables.of(
            orderedBlocks = initialSpec.blockNames.map(::resolveBlock),
            orderedBiomes = initialSpec.biomeNames.map { biomeLookup.getOrThrow(resolveBiome(it)) }
        )
        val biomeSource = OffworldBiomeSource(tables, initialSpec)
        return OffworldChunkGenerator(biomeSource, tables, specFactory)
    }

    private fun resolveBlock(name: String): BlockState = when (name) {
        "air" -> Blocks.AIR.defaultBlockState()
        "stone" -> Blocks.STONE.defaultBlockState()
        "sand" -> Blocks.SAND.defaultBlockState()
        else -> error("unknown block name '$name' in WorldgenSpec; not in seam resolver table")
    }

    private fun resolveBiome(name: String): ResourceKey<Biome> = when (name) {
        "plains" -> Biomes.PLAINS
        "desert" -> Biomes.DESERT
        else -> error("unknown biome name '$name' in WorldgenSpec; not in seam resolver table")
    }
}