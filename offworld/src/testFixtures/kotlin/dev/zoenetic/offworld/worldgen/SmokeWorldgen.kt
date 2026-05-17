package dev.zoenetic.offworld.worldgen

import dev.zoenetic.offworld.worldgen.fields.FractalField

object SmokeWorldgen {

    private const val AIR = 0
    private const val STONE = 1
    private const val SAND = 2

    private val PLAINS = BiomeId(0)
    private val DESERT = BiomeId(1)

    private const val MIN_Y = -64
    private const val HEIGHT = 384
    private const val SURFACE_Y = 64

    private const val TERRAIN_AMPLITUDE = 5.0
    private const val TERRAIN_PERIOD = 96.0
    private const val CLIMATE_PERIOD = 256.0
    private const val DUNE_AMPLITUDE = 16.0
    private const val DUNE_PERIOD = 32.0
    private const val DESERT_BIAS = 5.0
    private const val SAND_DEPTH = 4

    fun build(seed: Long): WorldgenSpec {

        val tempField = FractalField(seed, period = CLIMATE_PERIOD, columnConstant = true)
        val humidField = FractalField(seed + 2, period = CLIMATE_PERIOD, columnConstant = true)

        val climate = object : ClimateSpace {
            override val axes = setOf(StandardAxes.TEMPERATURE, StandardAxes.HUMIDITY)
            override fun sampleAt(x: Int, y: Int, z: Int): ClimateSample =
                ClimateSample.of(
                    mapOf(
                        StandardAxes.TEMPERATURE to tempField.sample(x.toDouble(), 0.0, z.toDouble()),
                        StandardAxes.HUMIDITY to humidField.sample(x.toDouble(), 0.0, z.toDouble())
                    ),
                )
        }

        val resolver = ParameterTreeResolver.create(
            entries = listOf(
                ParameterTreeResolver.BiomeEntry(
                    biome = PLAINS,
                    ideal = mapOf(
                        StandardAxes.TEMPERATURE to -0.3,
                        StandardAxes.HUMIDITY to 0.3,
                    ),
                    ranges = mapOf(
                        StandardAxes.TEMPERATURE to -0.8..0.2,
                        StandardAxes.HUMIDITY to -0.2..0.8,
                    ),
                ),
                ParameterTreeResolver.BiomeEntry(
                    biome = DESERT,
                    ideal = mapOf(
                        StandardAxes.TEMPERATURE to 0.3,
                        StandardAxes.HUMIDITY to -0.3,
                    ),
                    ranges = mapOf(
                        StandardAxes.TEMPERATURE to -0.2..0.8,
                        StandardAxes.HUMIDITY to -0.8..0.2,
                    ),
                ),
            ),
            axes = listOf(StandardAxes.TEMPERATURE, StandardAxes.HUMIDITY),
            k = 2,
            sigma = 0.7,
        )

        val terrainField = FractalField(seed + 1, period = TERRAIN_PERIOD, columnConstant = true)

        val skeleton = object : DensityNode {
            override val validContexts = setOf(SamplingContext.PER_BLOCK)
            override fun eval(ctx: EvalCtx): Double {
                val heightOffset = terrainField.sample(ctx.x, 0.0, ctx.z) * TERRAIN_AMPLITUDE
                return (SURFACE_Y + heightOffset) - ctx.y
            }
        }

        val duneField = FractalField(seed + 3, period = DUNE_PERIOD, columnConstant = true)

        val plainsProfile = object : TerrainProfile {
            override fun modulation(ctx: EvalCtx): Double = 0.0
        }

        val desertProfile = object : TerrainProfile {
            override fun modulation(ctx: EvalCtx): Double =
                DESERT_BIAS + duneField.sample(ctx.x, 0.0, ctx.z) * DUNE_AMPLITUDE
        }

        val terrain = DefaultTerrainShaper(
            skeleton = skeleton,
            profileOf = { biomeId ->
                when (biomeId) {
                    PLAINS -> plainsProfile
                    DESERT -> desertProfile
                    else -> error("no terrain profile for BiomeId(${biomeId.raw})")
                }
            },
        )

        val surface = SurfaceRule.Sequence(
            listOf(
                SurfaceRule.Conditioned(
                    cond = { ctx ->
                        val w = ctx.weightOf(DESERT)
                        w > 0.5 && ctx.stoneDepth < ((w - 0.5) * 2.0 * SAND_DEPTH).toInt()
                        },
                    then = SurfaceRule.Place { _, -> BlockId(SAND)},
                ),
                SurfaceRule.Place { _, -> BlockId(STONE)},
            ),
        )

        val carver = TunnelCarver()

        val spikeFeature = StoneSpikeFeature(
            stone = BlockId(STONE),
            air = BlockId(AIR),
        )

        val randomFactory: (WorldContext, ChunkPos, String) -> PositionalRandom =
            { worldCtx, pos, id ->
                var s = worldCtx.seed
                s = s xor (pos.x.toLong() * 0x9E3779B97F4A7C15uL.toLong())
                s = s xor (pos.z.toLong() * 0xC2B2AE3D27D4EB4FuL.toLong())
                s = s xor (id.hashCode().toLong() * 0xBF58476D1CE4E5B9uL.toLong())
                object : PositionalRandom {
                    override fun nextInt(bound: Int): Int {
                        require(bound > 0) { "bound must be positive" }
                        s = s * 6364136223846793005L + 1442695040888963407L
                        return ((s ushr 33).toInt() and 0x7fffffff) % bound
                    }
                    override fun nextDouble(): Double {
                        s = s * 6364136223846793005L + 1442695040888963407L
                        return ((s ushr 11) and 0x1FFFFFFFFFFFFFL).toDouble() / (1L shl 53)
                    }
                }
            }

        val pipeline = WorldgenPipeline(
            climate = climate,
            biomes = resolver,
            terrain = terrain,
            surface = surface,
            carvers = listOf(carver),
            features = listOf(spikeFeature),
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
            blockNames = listOf("air", "stone", "sand"),
            biomeNames = listOf("plains", "desert"),
        )
    }
}