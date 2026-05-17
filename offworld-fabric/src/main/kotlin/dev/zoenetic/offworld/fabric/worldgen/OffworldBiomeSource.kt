package dev.zoenetic.offworld.fabric.worldgen

import com.mojang.serialization.MapCodec
import dev.zoenetic.offworld.worldgen.ClimateSpace
import dev.zoenetic.offworld.worldgen.WorldgenSpec
import net.minecraft.core.Holder
import net.minecraft.core.QuartPos
import net.minecraft.world.level.biome.Biome
import net.minecraft.world.level.biome.BiomeSource
import net.minecraft.world.level.biome.Climate
import java.util.stream.Stream
import dev.zoenetic.offworld.worldgen.BiomeResolver as OffworldBiomeResolver

class OffworldBiomeSource(
    private val tables: SeamTables,
    initialSpec: WorldgenSpec,
) : BiomeSource() {

    @Volatile
    var spec: WorldgenSpec = initialSpec
        private set

    fun rebuild(newSpec: WorldgenSpec) {
        spec = newSpec
    }

    override fun collectPossibleBiomes(): Stream<Holder<Biome>> =
        tables.allBiomes().stream()

    override fun getNoiseBiome(
        quartX: Int,
        quartY: Int,
        quartZ: Int,
        sampler: Climate.Sampler,
    ): Holder<Biome> {
        val s = spec
        val bx = QuartPos.toBlock(quartX)
        val by = QuartPos.toBlock(quartY)
        val bz = QuartPos.toBlock(quartZ)
        val bc = s.biomes.resolve(s.climate.sampleAt(bx, by, bz))
        return tables.biome(bc.primary)
    }

    protected override fun codec(): MapCodec<out BiomeSource> =
        error("OffworldBiomeSource codec not wired yet!")
}