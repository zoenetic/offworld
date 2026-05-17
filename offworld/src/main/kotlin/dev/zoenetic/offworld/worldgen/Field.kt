package dev.zoenetic.offworld.worldgen

interface Field {
    val meta: FieldMeta
    fun sample(x: Double, y: Double, z: Double): Double
}

data class FieldMeta(
    val nativeResolution: Int,
    val interpolable: Boolean,
    val columnConstant: Boolean,
) {
    init {
        require(nativeResolution >= 1) {
            "nativeResolution must be >= 1, was $nativeResolution"
        }
    }

    companion object {
        fun continuous(resolution: Int = 1) =
            FieldMeta(resolution, interpolable = true, columnConstant = false)
    }
}

data class Region(
    val minX: Int, val minY: Int, val minZ: Int,
    val sizeX: Int, val sizeY: Int, val sizeZ: Int,
) {
    init {
        require(sizeX > 0 && sizeY > 0 && sizeZ > 0) {
            "region size must be positive, was ($sizeX, $sizeY, $sizeZ)"
        }
    }
}

interface Sampler {
    fun realize(field: Field, region: Region): RealizedField
}

interface RealizedField {
    operator fun get(localX: Int, localY: Int, localZ: Int): Double
}

private fun combineMeta(a: FieldMeta, b: FieldMeta) = FieldMeta(
    nativeResolution = minOf(a.nativeResolution, b.nativeResolution),
    interpolable = a.interpolable && b.interpolable,
    columnConstant = a.columnConstant && b.columnConstant,
)

class Add(private val a: Field, private val b: Field) : Field {
    override val meta = combineMeta(a.meta, b.meta)
    override fun sample(x: Double, y: Double, z: Double) =
        a.sample(x, y, z) + b.sample(x, y, z)
}

class Mul(private val a: Field, private val b: Field) : Field {
    override val meta = combineMeta(a.meta, b.meta)
    override fun sample(x: Double, y: Double, z: Double) =
        a.sample(x, y, z) * b.sample(x, y, z)
}

class Scale(private val a: Field, private val k: Double) : Field {
    override val meta = a.meta
    override fun sample(x: Double, y: Double, z: Double) = a.sample(x, y, z) * k
}

class Clamp(
    private val a: Field,
    private val lo: Double,
    private val hi: Double,
) : Field {
    init { require(lo <= hi) { "lo must be <= hi; was [$lo, $hi]" } }
    override val meta = a.meta
    override fun sample(x: Double, y: Double, z: Double) =
        a.sample(x, y, z).coerceIn(lo, hi)
}

class Spline(
    private val a: Field,
    points: List<Pair<Double, Double>>,
) : Field {
    private val xs = points.map { it.first }.toDoubleArray()
    private val ys = points.map { it.second }.toDoubleArray()

    init {
        require(xs.size >= 2) { "spline needs >= 2 points" }
        for (i in 1 until xs.size) {
            require(xs[i] > xs[i - 1]) { "spline xs must strictly increase" }
        }
    }

    override val meta = a.meta
    override fun sample(x: Double, y: Double, z: Double): Double {
        val v = a.sample(x, y, z)
        if (v <= xs[0]) return ys[0]
        if (v >= xs[xs.size - 1]) return ys[ys.size - 1]
        var i = 1
        while (v > xs[i]) i++
        val t = (v - xs[i - 1]) / (xs[i] - xs[i - 1])
        return ys[i - 1] + t * (ys[i] - ys[i - 1])
    }
}