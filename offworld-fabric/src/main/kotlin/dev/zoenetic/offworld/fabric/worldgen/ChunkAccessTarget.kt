package dev.zoenetic.offworld.fabric.worldgen

import dev.zoenetic.offworld.worldgen.BiomeId
import dev.zoenetic.offworld.worldgen.BlockId
import dev.zoenetic.offworld.worldgen.ChunkPos
import dev.zoenetic.offworld.worldgen.ChunkTarget
import net.minecraft.core.BlockPos
import net.minecraft.world.level.chunk.ChunkAccess

class ChunkAccessTarget(
    private val chunk: ChunkAccess,
    private val tables: SeamTables,
    override val pos: ChunkPos,
    private val minY: Int,
    private val maxY: Int,
) : ChunkTarget {

    private fun inChunk(x: Int, z: Int): Boolean {
        val bx = pos.minBlockX; val bz = pos.minBlockZ
        return x in bx..bx + 15 && z in bz..bz + 15
    }

    override fun setBlock(x: Int, y: Int, z: Int, block: BlockId) {
        if (!inChunk(x, z) || y !in minY until maxY) return
        chunk.setBlockState(BlockPos(x, y, z), tables.block(block))
    }

    override fun getBlock(x: Int, y: Int, z: Int): BlockId {
        if (!inChunk(x, z) || y !in minY until maxY) {
            error("getBlock out of range at ($x,$y,$z); core must not read there")
        }
        return tables.blockId(chunk.getBlockState(BlockPos(x, y, z)))
    }
}