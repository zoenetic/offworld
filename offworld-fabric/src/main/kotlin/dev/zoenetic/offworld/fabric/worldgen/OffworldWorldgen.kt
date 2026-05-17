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
        spec: WorldgenSpec,
        biomeLookup: HolderGetter<Biome>,
    ): OffworldChunkGenerator {
        val tables = SeamTables.of(
            orderedBlocks = spec.blockNames.map(::resolveBlock),
            orderedBiomes = spec.biomeNames.map { biomeLookup.getOrThrow(resolveBiome(it)) }
        )
        val biomeSource = OffworldBiomeSource(spec.climate, spec.biomes, tables)
        return OffworldChunkGenerator(biomeSource, tables, spec.pipeline, spec.world)
    }

    private fun resolveBlock(name: String): BlockState = when (name) {
        "air" -> Blocks.AIR.defaultBlockState()
        "stone" -> Blocks.STONE.defaultBlockState()
        else -> error("unknown block name '$name' in WorldgenSpec; not in seam resolver table")
    }

    private fun resolveBiome(name: String): ResourceKey<Biome> = when (name) {
        "plains" -> Biomes.PLAINS
        else -> error("unknown biome name '$name' in WorldgenSpec; not in seam resolver table")
    }
}