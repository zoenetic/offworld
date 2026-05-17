package dev.zoenetic.offworld.worldgen

interface TerrainProfile {
    fun modulation(ctx: EvalCtx): Double
}

interface TerrainShaper {
    fun density(ctx: EvalCtx): Double
}

class DefaultTerrainShaper(
    private val skeleton: DensityNode,
    private val profileOf: (BiomeId) -> TerrainProfile,
) : TerrainShaper {

    override fun density(ctx: EvalCtx): Double {
        val skeletonCtx =
            if (ctx.biome == null) ctx
            else EvalCtx(ctx.x, ctx.y, ctx.z, ctx.world, biome = null)
        val base = skeleton.eval(skeletonCtx)
        val bc = ctx.biome ?: return base
        var mod = 0.0
        for (wb in bc.blend) {
            mod += wb.weight * profileOf(wb.biome).modulation(ctx)
        }
        return base + mod
    }

}