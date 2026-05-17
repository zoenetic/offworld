package dev.zoenetic.offworld.worldgen

private class ArrayRealized(
    private val region: Region,
    private val data: DoubleArray,
) : RealizedField {
    override fun get(localX: Int, localY: Int, localZ: Int): Double =
        data[localX + region.sizeX * (localZ + region.sizeZ * localY)]
}

class PerBlockSampler : Sampler {
    override fun realize(field: Field, region: Region): RealizedField {
        val data = DoubleArray(region.sizeX * region.sizeY * region.sizeZ)
        var i = 0
        for (y in 0 until region.sizeY)
            for (z in 0 until region.sizeZ)
                for (x in 0 until region.sizeX) {
                    data[i++] = field.sample(
                        (region.minX + x).toDouble(),
                        (region.minY + y).toDouble(),
                        (region.minZ + z).toDouble(),
                    )
                }
        return ArrayRealized(region, data)
    }
}

class ColumnSampler : Sampler {
    override fun realize(field: Field, region: Region): RealizedField {
        require(field.meta.columnConstant) {
            "ColumnSampler requires a columnConstant field"
        }
        val plane = DoubleArray(region.sizeX * region.sizeZ)
        var i = 0
        for (z in 0 until region.sizeZ)
            for (x in 0 until region.sizeX) {
                plane[i++] = field.sample(
                    (region.minX + x).toDouble(),
                    region.minY.toDouble(),
                    (region.minZ + z).toDouble(),
                )
            }
        return object : RealizedField {
            override fun get(localX: Int, localY: Int, localZ: Int) =
                plane[localZ * region.sizeX + localX]
        }
    }
}

class CellInterpolatedSampler(
    private val cellXZ: Int = 4,
    private val cellY: Int = 8,
) : Sampler {
    init { require(cellXZ >= 1 && cellY >= 1) }

    override fun realize(field: Field, region: Region): RealizedField {
        require(field.meta.interpolable) {
            "CellInterpolatedSampler requires an interpolable field"
        }
        val nx = region.sizeX / cellXZ + 2
        val ny = region.sizeY / cellY + 2
        val nz = region.sizeZ / cellXZ + 2
        val lat = DoubleArray(nx * ny * nz)
        fun li(ix: Int, iy: Int, iz: Int) = ix + nx * (iz + nz * iy)
        for (iy in 0 until ny)
            for (iz in 0 until nz)
                for (ix in 0 until nx) {
                    lat[li(ix, iy, iz)] = field.sample(
                        (region.minX + ix * cellXZ).toDouble(),
                        (region.minY + iy * cellY).toDouble(),
                        (region.minZ + iz * cellXZ).toDouble(),
                    )
                }
        return object : RealizedField {
            override fun get(localX: Int, localY: Int, localZ: Int): Double {
                val gx = localX / cellXZ
                val gy = localY / cellY
                val gz = localZ / cellXZ
                val fx = (localX % cellXZ) / cellXZ.toDouble()
                val fy = (localY % cellY) / cellY.toDouble()
                val fz = (localZ % cellXZ) / cellXZ.toDouble()
                fun c(dx: Int, dy: Int, dz: Int) = lat[li(gx + dx, gy + dy, gz + dz)]
                val c00 = c(0, 0, 0) * (1 - fx) + c(1, 0, 0) * fx
                val c10 = c(0, 1, 0) * (1 - fx) + c(1, 1, 0) * fx
                val c01 = c(0, 0, 1) * (1 - fx) + c(1, 0, 1) * fx
                val c11 = c(0, 1, 1) * (1 - fx) + c(1, 1, 1) * fx
                val c0 = c00 * (1 - fy) + c10 * fy
                val c1 = c01 * (1 - fy) + c11 * fy
                return c0 * (1 - fz) + c1 * fz
            }
        }
    }
}