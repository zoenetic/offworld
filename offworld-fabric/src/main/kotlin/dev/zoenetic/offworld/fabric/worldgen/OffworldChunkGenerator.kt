package dev.zoenetic.offworld.fabric.worldgen

import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import dev.zoenetic.offworld.worldgen.ChunkPos as OffworldChunkPos
import dev.zoenetic.offworld.worldgen.WorldContext
import dev.zoenetic.offworld.worldgen.WorldgenPipeline
import dev.zoenetic.offworld.worldgen.SmokeWorldgen
import dev.zoenetic.offworld.worldgen.WorldgenSpec
import net.minecraft.core.BlockPos
import net.minecraft.core.HolderLookup
import net.minecraft.core.registries.Registries
import net.minecraft.resources.RegistryOps
import net.minecraft.server.level.WorldGenRegion
import net.minecraft.world.level.LevelHeightAccessor
import net.minecraft.world.level.NoiseColumn
import net.minecraft.world.level.StructureManager
import net.minecraft.world.level.WorldGenLevel
import net.minecraft.world.level.biome.BiomeManager
import net.minecraft.world.level.chunk.ChunkAccess
import net.minecraft.world.level.chunk.ChunkGenerator
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState
import net.minecraft.world.level.levelgen.Heightmap
import net.minecraft.world.level.levelgen.RandomState
import net.minecraft.world.level.levelgen.blending.Blender
import net.minecraft.world.level.levelgen.structure.StructureSet
import java.util.concurrent.CompletableFuture

class OffworldChunkGenerator(
    private val offworldBiomeSource: OffworldBiomeSource,
    private val tables: SeamTables,
    private val specFactory: (Long) -> WorldgenSpec,
) : ChunkGenerator(offworldBiomeSource) {

    private val world: WorldContext get() = offworldBiomeSource.spec.world

    protected override fun codec(): MapCodec<out ChunkGenerator> = CODEC

    override fun createState(
        structureLookup: HolderLookup<StructureSet>,
        randomState: RandomState,
        seed: Long,
    ): ChunkGeneratorStructureState {
        offworldBiomeSource.rebuild(specFactory(seed))
        return super.createState(structureLookup, randomState, seed)
    }

    override fun fillFromNoise(
        blender: Blender,
        randomState: RandomState,
        structures: StructureManager,
        chunk: ChunkAccess,
    ): CompletableFuture<ChunkAccess> {
        val spec = offworldBiomeSource.spec
        val pos = OffworldChunkPos(chunk.pos.x, chunk.pos.z)
        val target = ChunkAccessTarget(chunk, tables, pos, world.minY, world.maxY)
        spec.pipeline.generate(pos, world, target)
        return CompletableFuture.completedFuture(chunk)
    }

    override fun buildSurface(
        region: WorldGenRegion,
        structures: StructureManager,
        randomState: RandomState,
        chunk: ChunkAccess,
    ) = Unit

    override fun applyCarvers(
        region: WorldGenRegion,
        seed: Long,
        randomState: RandomState,
        biomeManager: BiomeManager,
        structures: StructureManager,
        chunk: ChunkAccess,
    ) = Unit

    override fun applyBiomeDecoration(
        level: WorldGenLevel,
        chunk: ChunkAccess,
        structures: StructureManager,
    ) = Unit

    override fun spawnOriginalMobs(region: WorldGenRegion) = Unit

    override fun getGenDepth(): Int = world.height
    override fun getSeaLevel(): Int = world.minY + world.height / 2
    override fun getMinY(): Int = world.minY

    override fun getBaseHeight(
        x: Int,
        z: Int,
        type: Heightmap.Types,
        level: LevelHeightAccessor,
        randomState: RandomState,
    ): Int {
        val spec = offworldBiomeSource.spec
        return spec.pipeline.surfaceHeight(x, z, spec.world) + 1
    }

    override fun getBaseColumn(
        x: Int,
        z: Int,
        level: LevelHeightAccessor,
        randomState: RandomState,
    ): NoiseColumn = NoiseColumn(world.minY, emptyArray())

    override fun addDebugScreenInfo(
        info: MutableList<String>,
        randomState: RandomState,
        pos: BlockPos,
    ) = Unit

    companion object {
        val CODEC: MapCodec<OffworldChunkGenerator> = RecordCodecBuilder.mapCodec { instance ->
            instance.group(
                RegistryOps.retrieveGetter(Registries.BIOME),
            ).apply(instance) { biomes ->
                val spec = SmokeWorldgen.build(seed = 0L)
                OffworldWorldgen.assemble(SmokeWorldgen::build, biomes)
            }
        }
    }
}