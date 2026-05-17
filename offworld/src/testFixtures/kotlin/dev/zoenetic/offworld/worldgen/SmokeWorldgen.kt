package dev.zoenetic.offworld.worldgen

object SmokeWorldgen {

    private const val AIR = 0
    private const val STONE = 1

    private const val MIN_Y = -64
    private const val HEIGHT = 384
    private const val SURFACE_Y = 64

    fun build(seed: Long): WorldgenSpec {
        val climate = object : ClimateSpace {
            override val axes = setOf(StandardAxes.TEMPERATURE)
            override fun sampleAt(x: Int, y: Int, z: Int): ClimateSample =
                ClimateSample.of(mapOf(StandardAxes.TEMPERATURE to 0.0))
        }

        val singleBiome = BiomeContext.of(
            primary = BiomeId(0),
            blend = listOf(WeightedBiome(BiomeId(0), 1.0))
        )

        val resolver = object : BiomeResolver {
            override fun resolve(climate: ClimateSample): BiomeContext = singleBiome
        }

        val skeleton = object : DensityNode {
            override val validContexts = setOf(SamplingContext.PER_BLOCK)
            override fun eval(ctx: EvalCtx): Double = SURFACE_Y - ctx.y
        }

        val terrain = DefaultTerrainShaper(
            skeleton = skeleton,
            profileOf = { _ ->
                object : TerrainProfile {
                    override fun modulation(ctx: EvalCtx): Double = 0.0
                }
            }
        )

        val surface = SurfaceRule.Place { _ -> BlockId(STONE) }

        val randomFactory: (WorldContext, ChunkPos, String) -> PositionalRandom =
            { _, _, _ ->
                object : PositionalRandom {
                    override fun nextInt(bound: Int): Int = 0
                    override fun nextDouble(): Double = 0.0
                }
            }

        val pipeline = WorldgenPipeline(
            climate = climate,
            biomes = resolver,
            terrain = terrain,
            surface = surface,
            features = emptyList(),
            scheduler = TopoFeatureScheduler(),
            randomFactory = randomFactory,
            airBlock = BlockId(AIR),
        )

        val world = WorldContext(seed = seed, minY = MIN_Y, height = HEIGHT)

        return WorldgenSpec(
            pipeline = pipeline,
            world = world,
            climate = climate,
            biomes = resolver,
            blockNames = listOf("air", "stone"),
            biomeNames = listOf("plains"),
        )
    }
}