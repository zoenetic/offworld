package dev.zoenetic.offworld.worldgen

interface PlacedFeature {
    val id: String
    val provides: Set<String>
    val requires: Set<String>
    fun place(ctx: FeatureCtx)
}

class FeatureCtx(
    val chunk: ChunkPos,
    val biome: BiomeContext,
    val world: WorldContext,
    val target: ChunkTarget,
    val random: PositionalRandom,
)

interface PositionalRandom {
    fun nextInt(bound: Int): Int
    fun nextDouble(): Double
}

class FeatureGraphError(message: String) : IllegalStateException(message)

interface FeatureScheduler {
    fun order(features: List<PlacedFeature>): List<PlacedFeature>
}

class TopoFeatureScheduler : FeatureScheduler {
    override fun order(features: List<PlacedFeature>): List<PlacedFeature> {
        val byCap = HashMap<String, MutableList<PlacedFeature>>()
        for (f in features)
            for (cap in f.provides)
                byCap.getOrPut(cap) { mutableListOf() }.add(f)
        val indeg = HashMap<PlacedFeature, Int>()
        val edges = HashMap<PlacedFeature, MutableList<PlacedFeature>>()
        for (f in features) {
            indeg.putIfAbsent(f, 0)
            for (cap in f.requires) {
                byCap[cap]?.forEach { dep ->
                    edges.getOrPut(dep) { mutableListOf() }.add(f)
                    indeg[f] = (indeg[f] ?: 0) + 1
                }
            }
        }
        val queue = ArrayDeque(features.filter { indeg[it] == 0 })
        val out = ArrayList<PlacedFeature>(features.size)
        while (queue.isNotEmpty()) {
            val n = queue.removeFirst()
            out += n
            edges[n]?.forEach { m ->
                indeg[m] = indeg[m]!! - 1
                if (indeg[m] == 0) queue += m
            }
        }
        if (out.size != features.size) {
            throw FeatureGraphError(
                "capability cycle among features; " +
                    "${features.size - out.size} feature(s) unschedulable",
            )
        }
        return out
    }
}