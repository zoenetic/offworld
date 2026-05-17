package dev.zoenetic.offworld.fabric.worldgen

import dev.zoenetic.offworld.worldgen.BiomeId
import dev.zoenetic.offworld.worldgen.BlockId
import net.minecraft.core.Holder
import net.minecraft.world.level.biome.Biome
import net.minecraft.world.level.block.state.BlockState
import kotlin.apply

class SeamTables private constructor(
    private val blocks: Array<BlockState>,
    private val biomes: Array<Holder<Biome>>,
) {
    fun block(id: BlockId): BlockState = blocks[id.raw]
    fun biome(id: BiomeId): Holder<Biome> = biomes[id.raw]

    private val blockReverse: Map<BlockState, BlockId> =
        java.util.IdentityHashMap<BlockState, BlockId>().apply {
            blocks.forEachIndexed { i, s -> put(s, BlockId(i)) }
        }

    fun allBiomes(): List<Holder<Biome>> = biomes.asList()

    fun blockId(state: BlockState): BlockId =
        blockReverse[state]
            ?: error ("BlockState $state has no BlockId; not a worldgen block")

    companion object {
        fun of(
            orderedBlocks: List<BlockState>,
            orderedBiomes: List<Holder<Biome>>,
        ): SeamTables {
            require(orderedBlocks.isNotEmpty()) { "no worldgen blocks registered" }
            require(orderedBiomes.isNotEmpty()) { "no worldgen biomes registered" }
            return SeamTables(
                orderedBlocks.toTypedArray(),
                orderedBiomes.toTypedArray(),
            )
        }
    }
}