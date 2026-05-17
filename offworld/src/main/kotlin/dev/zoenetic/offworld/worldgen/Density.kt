package dev.zoenetic.offworld.worldgen

enum class SamplingContext { INTERPOLATED, CELL_ALIGNED, PER_BLOCK, COLUMN }

class EvalCtx(
    val x: Double,
    val y: Double,
    val z: Double,
    val world: WorldContext,
    val biome: BiomeContext?,
)

interface DensityNode {
    val validContexts: Set<SamplingContext>
    fun eval(ctx: EvalCtx): Double
}

class DensityGraphError(val path: String, message: String) :
        IllegalStateException("$path: $message")

object Density {
    fun checked(
        node: DensityNode,
        required: SamplingContext,
        path: String,
    ): DensityNode {
        if (required !in node.validContexts) {
            throw DensityGraphError(
                path,
                "node valid in ${node.validContexts} but parent requires $required",
            )
        }
        return node
    }
}

interface DensityOp {
    val id: String
    fun create(children: List<DensityNode>, params: OpParams): DensityNode
}

interface OpParams {
    fun double(key: String): Double
    fun int(key: String): Int
    fun string(key: String): String
    fun doubleList(key: String): List<Double>
}

interface DensityOpRegistry {
    fun register(op: DensityOp)
    fun get(id: String): DensityOp
}