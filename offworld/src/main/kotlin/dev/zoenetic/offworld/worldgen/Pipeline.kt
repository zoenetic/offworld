package dev.zoenetic.offworld.worldgen

class WorldgenPipeline(
    private val climate: ClimateSpace,
    private val biomes: BiomeResolver,
    private val terrain: TerrainShaper,
    private val surface: SurfaceRule,
    private val carvers: List<Carver>,
    private val features: List<PlacedFeature>,
    private val scheduler: FeatureScheduler,
    private val randomFactory: (WorldContext, ChunkPos, String) -> PositionalRandom,
    private val airBlock: BlockId,
    private val sampleStepY: Int = 4,
) {
    private val orderedFeatures: List<PlacedFeature> = scheduler.order(features)

    init {
        require(sampleStepY >= 1) { "sampleStepY must be >= 1; was $sampleStepY" }
    }

    fun generate(pos: ChunkPos, world: WorldContext, target: ChunkTarget) {
        val biomeGrid = Array(16) { arrayOfNulls<BiomeContext>(16) }
        for (lz in 0 until 16) {
            for (lx in 0 until 16) {
                val wx = pos.minBlockX + lx
                val wz = pos.minBlockZ + lz
                val sample = climate.sampleAt(wx, world.minY + world.height, wz)
                val bc = biomes.resolve(sample)
                biomeGrid[lx][lz] = bc
                val surfaceY = findSurfaceY(wx, wz, world, bc)
                for (y in world.minY..surfaceY) {
                    val stoneDepth = surfaceY - y
                    val sctx = SurfaceCtx(
                        x = wx, y = y, z = wz,
                        surfaceY = surfaceY,
                        stoneDepth = stoneDepth,
                        biome = bc,
                        world = world,
                    )
                    val block = surface.apply(sctx)
                        ?: error(
                            "surface rule produced no block at " +
                                "($wx,$y,$wz); the rule trtee must have a " +
                                    "terminal Place",
                        )
                    target.setBlock(wx, y, wz, block)
                }
            }
        }
        if (carvers.isNotEmpty()) {
            val mask = CarveMask(world.minY, world.height)
            for (carver in carvers) {
                carver.carve(pos, world, terrain, mask)
            }
            for (lz in 0 until 16) {
                for (lx in 0 until 16) {
                    val wx = pos.minBlockX + lx
                    val wz = pos.minBlockZ + lz
                    for (y in world.minY until world.maxY) {
                        if (mask.isCarved(lx, lz, y)) {
                            target.setBlock(wx, y, wz, airBlock)
                        }
                    }
                }
            }
        }
        for (f in orderedFeatures) {
            val ctx = FeatureCtx(
                chunk = pos,
                biome = biomeGrid[8][8]!!,
                world = world,
                target = target,
                random = randomFactory(world, pos, f.id),
            )
            f.place(ctx)
        }
    }

    private fun findSurfaceY(
        wx: Int,
        wz: Int,
        world: WorldContext,
        bc: BiomeContext,
    ): Int {
        fun solid(y: Int): Boolean {
            val d = terrain.density(
                EvalCtx(wx.toDouble(), y.toDouble(), wz.toDouble(), world, bc),
            )
            return d > 0.0
        }
        var yHi = world.maxY - 1
        var y = yHi
        while (y > world.minY && !solid(y)) y -= sampleStepY
        if (y <= world.minY) return world.minY
        var refined = y
        for (probe in y + 1..minOf(y + sampleStepY, world.maxY - 1)) {
            if (solid(probe)) refined = probe else break
        }
        return refined
    }
}