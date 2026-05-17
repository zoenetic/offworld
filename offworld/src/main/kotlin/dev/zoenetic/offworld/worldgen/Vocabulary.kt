package dev.zoenetic.offworld.worldgen

@JvmInline
value class BlockId(val raw: Int)

@JvmInline
value class BiomeId(val raw: Int)

data class ChunkPos(val x: Int, val z: Int) {
    val minBlockX: Int get() = x shl 4
    val minBlockZ: Int get() = z shl 4
}

class WorldContext(
    val seed: Long,
    val minY: Int,
    val height: Int,
) {
    init {
        require(height > 0) { "height must be positive, was $height" }
    }

    val maxY: Int get() = minY + height
}

interface ChunkTarget {
    val pos: ChunkPos
    fun setBlock(x: Int, y: Int, z: Int, block: BlockId)
    fun getBlock(x: Int, y: Int, z: Int): BlockId
}