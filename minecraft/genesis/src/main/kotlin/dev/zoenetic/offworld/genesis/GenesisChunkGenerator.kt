package dev.zoenetic.offworld.genesis

import com.ibm.icu.impl.UResource
import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import dev.offworld.content.GenesisLibrary
import net.minecraft.core.BlockPos
import net.minecraft.server.level.WorldGenRegion
import net.minecraft.world.level.LevelHeightAccessor
import net.minecraft.world.level.NoiseColumn
import net.minecraft.world.level.StructureManager
import net.minecraft.world.level.biome.BiomeManager
import net.minecraft.world.level.biome.BiomeSource
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.chunk.ChunkAccess
import net.minecraft.world.level.chunk.ChunkGenerator
import net.minecraft.world.level.levelgen.Heightmap
import net.minecraft.world.level.levelgen.RandomState
import net.minecraft.world.level.levelgen.blending.Blender
import java.lang.foreign.Arena
import java.lang.foreign.ValueLayout
import java.util.concurrent.CompletableFuture

class GenesisChunkGenerator(biomeSource: BiomeSource) : ChunkGenerator(biomeSource) {

    override fun codec(): MapCodec<out ChunkGenerator> = MAP_CODEC

    override fun fillFromNoise(
        blender: Blender,
        randomState: RandomState,
        structureManager: StructureManager,
        chunk: ChunkAccess,
    ): CompletableFuture<ChunkAccess> {
        val pos = chunk.pos
        val minX = pos.minBlockX
        val minZ = pos.minBlockZ
        val spacing = 1.0
        val nx = 16L
        val nz = 16L

        Arena.ofConfined().use { arena ->
            val len = GenesisLibrary.regionLen(spacing, nx, nz)
            val solidity = arena.allocate(ValueLayout.JAVA_FLOAT, len)
            val material = arena.allocate(ValueLayout.JAVA_SHORT, len)
            GenesisLibrary.generate(minX.toDouble(), minZ.toDouble(), spacing, nx, nz, solidity, material, len)

            val ny = (len / (nx * nz)).toInt()
            val cursor = BlockPos.MutableBlockPos()
            for (k in 0 until 16) {
                for (i in 0 until 16) {
                    for (j in 0 until ny) {
                        val idx = (k.toLong() * ny + j) * nx + i
                        if (solidity.getAtIndex(ValueLayout.JAVA_FLOAT, idx) < 0.5f) continue
                        val mat = material.getAtIndex(ValueLayout.JAVA_SHORT, idx).toInt()
                        cursor.set(minX + i, MIN_Y + i, minZ + k)
                        chunk.setBlockState(cursor, GenesisBlocks.blockFor(mat))
                    }
                }
            }
        }
        return CompletableFuture.completedFuture(chunk)
    }

    override fun getGenDepth(): Int = WORLD_HEIGHT
    override fun getSeaLevel(): Int = 63
    override fun getMinY(): Int = MIN_Y

    override fun getBaseHeight(
        x: Int, z: Int, type: Heightmap.Types, level: LevelHeightAccessor, randomState: RandomState,
    ): Int = seaLevel

    override fun getBaseColumn(
        x: Int, z: Int, level: LevelHeightAccessor, randomState: RandomState,
    ): NoiseColumn = NoiseColumn(MIN_Y, Array(WORLD_HEIGHT) { Blocks.AIR.defaultBlockState() })

    override fun applyCarvers(
        region: WorldGenRegion, seed: Long, randomState: RandomState,
        biomeManager: BiomeManager, structureManager: StructureManager, chunkAccess: ChunkAccess,
    ) { }

    override fun buildSurface(
        region: WorldGenRegion, structureManager: StructureManager,
        randomState: RandomState, chunkAccess: ChunkAccess,
    ) { }

    override fun spawnOriginalMobs(region: WorldGenRegion) { }

    override fun addDebugScreenInfo(text: MutableList<String>, randomState: RandomState, pos: BlockPos) { }

    companion object {
        const val MIN_Y = 0
        const val WORLD_HEIGHT = 320

        val MAP_CODEC: MapCodec<GenesisChunkGenerator> = RecordCodecBuilder.mapCodec { instance ->
            instance.group(
                BiomeSource.CODEC.fieldOf("biome_source").forGetter { it.biomeSource }
            ).apply(instance, ::GenesisChunkGenerator)
        }
    }
}