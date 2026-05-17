package dev.zoenetic.offworld.worldgen

data class WorldgenSpec(
    val pipeline: WorldgenPipeline,
    val world: WorldContext,
    val climate: ClimateSpace,
    val biomes: BiomeResolver,
    val blockNames: List<String>,
    val biomeNames: List<String>,
)