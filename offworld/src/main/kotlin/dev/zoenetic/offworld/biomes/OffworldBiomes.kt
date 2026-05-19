package dev.zoenetic.offworld.biomes

import dev.zoenetic.offworld.block.OffworldBlocks
import dev.zoenetic.offworld.BiomeKey
import dev.zoenetic.offworld.BlockKey
import dev.zoenetic.offworld.worldgen.ClimateAxis
import dev.zoenetic.offworld.worldgen.StandardAxes.HUMIDITY
import dev.zoenetic.offworld.worldgen.StandardAxes.TEMPERATURE

data class ClimateProfile(
    val ideal: Map<ClimateAxis, Double>,
    val ranges: Map<ClimateAxis, ClosedFloatingPointRange<Double>>,
)

data class BiomeSurfaceDef(val top: BlockKey)

data class OffworldBiome(
    val key: BiomeKey,
    val climate: ClimateProfile,
    val surface: BiomeSurfaceDef,
)

object OffworldBiomes {

    val PLAINS = OffworldBiome(
        key = BiomeKey("plains"),
        climate = ClimateProfile(
            ideal = mapOf(
                TEMPERATURE to -0.3,
                HUMIDITY    to  0.3,
            ),
            ranges = mapOf(
                TEMPERATURE to -0.8..0.2,
                HUMIDITY    to -0.2..0.8,
            ),
        ),
        surface = BiomeSurfaceDef(top = OffworldBlocks.CHERT),
    )

    val DESERT = OffworldBiome(
        key = BiomeKey("desert"),
        climate = ClimateProfile(
            ideal = mapOf(
                TEMPERATURE to  0.3,
                HUMIDITY    to -0.3,
            ),
            ranges = mapOf(
                TEMPERATURE to -0.2..0.8,
                HUMIDITY    to -0.8..0.2,
            ),
        ),
        surface = BiomeSurfaceDef(top = OffworldBlocks.SILICA_SAND),
    )

    val ALL: List<OffworldBiome> = listOf(PLAINS, DESERT)
    val byKey: Map<BiomeKey, OffworldBiome> = ALL.associateBy { it.key }
}