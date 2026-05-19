package dev.zoenetic.offworld.worldgen

import dev.zoenetic.offworld.BiomeKey
import dev.zoenetic.offworld.BlockKey

data class BiomeSurface(
    val biome: BiomeKey,
    val top: BlockKey,
)

data class BlockProfile(
    val block: BlockKey,
    val sub: BlockKey? = null,
    val subDepth: Int = 3,
)

data class BlockEdge(
    val between: Pair<BlockKey, BlockKey>,
    val edge: BlockKey,
    val depth: Int = 1,
    val minWeight: Double = 0.1,
)

interface CatalogResolver {
    fun blockId(key: BlockKey): BlockId
    fun biomeId(key: BiomeKey): BiomeId
}

class IndexedCatalogResolver(
    blocks: List<BlockKey>,
    biomes: List<BiomeKey>,
) : CatalogResolver {
    private val blockIdx: Map<BlockKey, Int> =
        blocks.withIndex().associate { (i, k) -> k to i }
    private val biomeIdx: Map<BiomeKey, Int> =
        biomes.withIndex().associate { (i, k) -> k to i }

    override fun blockId(key: BlockKey): BlockId =
        BlockId(blockIdx[key] ?: error("block $key not in spec's blocks list"))
    override fun biomeId(key: BiomeKey): BiomeId =
        BiomeId(biomeIdx[key] ?: error("biome $key not in spec's biomes list"))
}

private data class EdgeIndexKey(val a: BlockKey, val b: BlockKey) {
    companion object {
        fun of(p: Pair<BlockKey, BlockKey>): EdgeIndexKey =
            if (p.first.id <= p.second.id) EdgeIndexKey(p.first, p.second)
            else EdgeIndexKey(p.second, p.first)
    }
}

data class SurfaceProgram(
    val floor: BlockKey,
    val biomes: List<BiomeSurface>,
    val profiles: List<BlockProfile>,
    val edges: List<BlockEdge>,
)

fun SurfaceProgram.toRule(cat: CatalogResolver): SurfaceRule {
    val topByBiome: Map<BiomeId, BlockKey> =
        biomes.associate { cat.biomeId(it.biome) to it.top }
    val profileByKey: Map<BlockKey, BlockProfile> =
        profiles.associateBy { it.block }
    val edgeByKey: Map<EdgeIndexKey, BlockEdge> =
        edges.associateBy { EdgeIndexKey.of(it.between) }
    val floorId = cat.blockId(floor)

    return SurfaceRule.Place { ctx ->
        val primaryTop = topByBiome[ctx.biome.primary]
            ?: error("no BiomeSurface registered for biome ${ctx.biome.primary}")

        val edgeHit = ctx.biome.blend.asSequence()
            .filter { it.biome != ctx.biome.primary }
            .mapNotNull { wb ->
                val otherTop = topByBiome[wb.biome] ?: return@mapNotNull null
                if (otherTop == primaryTop) return@mapNotNull null
                val def = edgeByKey[EdgeIndexKey.of(primaryTop to otherTop)] ?: return@mapNotNull null
                wb to def
            }
            .firstOrNull { (wb, def) -> wb.weight >= def.minWeight && ctx.dither < wb.weight }

        val (effTop, edgeDepth) =
            if (edgeHit != null) edgeHit.second.edge to edgeHit.second.depth
            else primaryTop to 1

        when {
            ctx.stoneDepth < edgeDepth -> cat.blockId(effTop)
            else -> {
                val prof = profileByKey[effTop]
                val below = ctx.stoneDepth - edgeDepth + 1
                if (prof?.sub != null && below <= prof.subDepth) cat.blockId(prof.sub)
                else floorId
            }
        }
    }
}

private data class EdgeKey(val a: String, val b: String) {
    companion object {
        fun of(p: Pair<String, String>): EdgeKey =
            EdgeKey(minOf(p.first, p.second), maxOf(p.first, p.second))
    }
}

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