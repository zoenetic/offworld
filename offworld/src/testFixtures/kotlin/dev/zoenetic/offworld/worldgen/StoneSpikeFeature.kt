package dev.zoenetic.offworld.worldgen

class StoneSpikeFeature(
    private val stone: BlockId,
    private val air: BlockId,
    private val attemptsPerChunk: Int = 2,
    private val minHeight: Int = 4,
    private val maxHeight: Int = 10,
) : PlacedFeature {

    init {
        require(attemptsPerChunk >= 0) {
            "attemptsPerChunk must be non-negative; was $attemptsPerChunk"
        }
        require(minHeight >= 1) { "minHeight must be >= 1; was $minHeight" }
        require(maxHeight >= minHeight) { "maxHeight must be >= minHeight; was $maxHeight" }
    }

    override val id: String = "offworld:stone_spike"
    override val provides: Set<String> = emptySet()
    override val requires: Set<String> = emptySet()

    override fun place(ctx: FeatureCtx) {
        repeat(attemptsPerChunk) {
            val lx = ctx.random.nextInt(16)
            val lz = ctx.random.nextInt(16)
            val wx = ctx.chunk.minBlockX + lx
            val wz = ctx.chunk.minBlockZ + lz
            val surfaceY = findTopSolid(ctx, wx, wz) ?: return@repeat
            val height = minHeight + ctx.random.nextInt(maxHeight - minHeight + 1)
            for (h in 1..height) {
                val y = surfaceY + h
                if (y >= ctx.world.maxY) break
                ctx.target.setBlock(wx, y, wz, stone)
            }
        }
    }

    private fun findTopSolid(ctx: FeatureCtx, wx: Int, wz: Int): Int? {
        for (y in ctx.world.maxY - 1 downTo ctx.world.minY) {
            if (ctx.target.getBlock(wx, y, wz) != air) return y
        }
        return null
    }
}