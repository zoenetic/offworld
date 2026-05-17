package dev.zoenetic.offworld.worldgen.fields

import dev.zoenetic.offworld.worldgen.Field
import dev.zoenetic.offworld.worldgen.FieldMeta
import kotlin.math.floor

class PerlinField(
    seed: Long,
    private val period: Double = 64.0,
    private val columnConstant: Boolean = false,
) : Field {

    override val meta = FieldMeta(
        nativeResolution = maxOf(1, (period / 4.0).toInt()),
        interpolable = true,
        columnConstant = columnConstant,
    )

    private val perm = IntArray(512)

    init {
        require(period > 0.0) { "period must be positive; was $period" }
        val p = IntArray(256) { it }
        var s = seed xor 0x5DEECE66DL
        for (i in 255 downTo 1) {
            s = s * 6364136223846793005L + 1442695040888963407L
            val j = ((s ushr 33).toInt() and 0x7fffffff) % (i + 1)
            val t = p[i]; p[i] = p[j]; p[j] = t
        }
        for (i in 0 until 512) perm[i] = p[i and 255]
    }

    private fun fade(t: Double) = t * t * t * (t * (t * 6 - 15) + 10)
    private fun lerp(a: Double, b: Double, t: Double) = a + t * (b - a)

    private fun grad(hash: Int, x: Double, y: Double, z: Double): Double =
        when (hash and 15) {
            0 -> x + y; 1 -> -x + y; 2 -> x - y; 3 -> -x - y
            4 -> x + z; 5 -> -x + z; 6 -> x - z; 7 -> -x - z
            8 -> y + z; 9 -> -y + z; 10 -> y - z; 11 -> -y - z
            12 -> y + x; 13 -> -y + z; 14 -> y - x; else -> -y - z
        }

    override fun sample(x: Double, y: Double, z: Double): Double {
        val sx = x / period
        val sy = if (columnConstant) 0.0 else y / period
        val sz = z / period

        val xi = floor(sx).toInt() and 255
        val yi = floor(sy).toInt() and 255
        val zi = floor(sz).toInt() and 255
        val xf = sx - floor(sx)
        val yf = sy - floor(sy)
        val zf = sz - floor(sz)
        val u = fade(xf); val v = fade(yf); val w = fade(zf)

        val a = perm[xi] + yi
        val aa = perm[a] + zi
        val ab = perm[a + 1] + zi
        val b = perm[xi + 1] + yi
        val ba = perm[b] + zi
        val bb = perm[b + 1] + zi

        return lerp(
            lerp(
                lerp(grad(perm[aa], xf, yf, zf),
                    grad(perm[ba], xf - 1, yf, zf), u),
                lerp(grad(perm[ab], xf, yf - 1, zf),
                    grad(perm[bb], xf - 1, yf - 1, zf), u),
                v,
            ),
            lerp(
                lerp(grad(perm[aa + 1], xf, yf, zf - 1),
                    grad(perm[ba + 1], xf - 1, yf, zf - 1), u),
                lerp(grad(perm[ab + 1], xf, yf - 1, zf - 1),
                    grad(perm[bb + 1], xf - 1, yf - 1, zf - 1), u),
                v,
            ),
            w,
        )
    }
}