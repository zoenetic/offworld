package dev.zoenetic.offworld.worldgen

class SurfaceCtx(
    val x: Int, val y: Int, val z: Int,
    val surfaceY: Int,
    val stoneDepth: Int,
    val biome: BiomeContext,
    val world: WorldContext,
    val dither: Double,
)

fun interface SurfaceCondition { fun test(ctx: SurfaceCtx): Boolean }
fun interface BlockProvider { fun pick(ctx: SurfaceCtx): BlockId }

sealed interface SurfaceRule {
    fun apply(ctx: SurfaceCtx): BlockId?

    class Sequence(private val rules: List<SurfaceRule>) : SurfaceRule {
        override fun apply(ctx: SurfaceCtx) : BlockId? =
            rules.firstNotNullOfOrNull { it.apply(ctx) }
    }

    class Conditioned(
        private val cond: SurfaceCondition,
        private val then: SurfaceRule
    ) : SurfaceRule {
        override fun apply(ctx: SurfaceCtx) : BlockId? =
            if (cond.test(ctx)) then.apply(ctx) else null
    }

    class Place(private val provider: BlockProvider) : SurfaceRule {
        override fun apply(ctx: SurfaceCtx) : BlockId = provider.pick(ctx)
    }
}

fun SurfaceCtx.weightOf(biome: BiomeId): Double =
    biome.let { b -> this.biome.blend.firstOrNull { it.biome == b }?.weight ?: 0.0 }