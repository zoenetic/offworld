package dev.zoenetic.offworld.genesis

import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState

object GenesisBlocks {
    fun blockFor(material: Int): BlockState = when (material) {
        1 -> Blocks.BEDROCK.defaultBlockState() // bedrock
        2 -> Blocks.CALCITE.defaultBlockState() // limestone
        3 -> Blocks.DEEPSLATE.defaultBlockState() // shale
        4 -> Blocks.SANDSTONE.defaultBlockState() // sandstone
        5 -> Blocks.STONE.defaultBlockState() // stone
        6 -> Blocks.GRAVEL.defaultBlockState() // scree
        7 -> Blocks.DIRT.defaultBlockState() // soil
        8 -> Blocks.SAND.defaultBlockState() // sand
        9 -> Blocks.SNOW_BLOCK.defaultBlockState() // snow
        else -> Blocks.AIR.defaultBlockState()
    }
}