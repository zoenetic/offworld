package dev.zoenetic.offworld.fabric.worldgen

import dev.zoenetic.offworld.BiomeKey
import dev.zoenetic.offworld.BlockKey
import dev.zoenetic.offworld.worldgen.WorldgenSpec
import net.minecraft.core.HolderGetter
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey
import net.minecraft.world.level.biome.Biome
import net.minecraft.world.level.block.state.BlockState

object OffworldWorldgen {

    fun assemble(
        specFactory: (Long) -> WorldgenSpec,
        biomeLookup: HolderGetter<Biome>,
    ): OffworldChunkGenerator {
        val initialSpec = specFactory(0L)
        val tables = SeamTables.of(
            orderedBlocks = initialSpec.blockKeys.map(::resolveBlock),
            orderedBiomes = initialSpec.biomeKeys.map { biomeLookup.getOrThrow(resolveBiome(it)) },
        )
        val biomeSource = OffworldBiomeSource(tables, initialSpec)
        return OffworldChunkGenerator(biomeSource, tables, specFactory)
    }

    private fun resolveBlock(key: BlockKey): BlockState {
        val id = Identifier.fromNamespaceAndPath(key.namespace, key.path)
        val block = BuiltInRegistries.BLOCK.getValue(id)
            ?: error("Unknown block '$key' — not in BLOCK registry " +
                    "(forgot to register, or namespace typo)")
        return block.defaultBlockState()
    }

    private fun resolveBiome(key: BiomeKey): ResourceKey<Biome> =
        ResourceKey.create(Registries.BIOME, Identifier.fromNamespaceAndPath(key.namespace, key.path))
}