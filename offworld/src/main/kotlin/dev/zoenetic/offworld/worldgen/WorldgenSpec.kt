package dev.zoenetic.offworld.worldgen

import dev.zoenetic.offworld.BiomeKey
import dev.zoenetic.offworld.BlockKey

data class WorldgenSpec(
    val pipeline: WorldgenPipeline,
    val world: WorldContext,
    val climate: ClimateSpace,
    val biomes: BiomeResolver,
    val blockKeys: List<BlockKey>,
    val biomeKeys: List<BiomeKey>,
)