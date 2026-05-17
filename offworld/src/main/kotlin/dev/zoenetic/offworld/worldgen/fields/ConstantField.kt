package dev.zoenetic.offworld.worldgen.fields

import dev.zoenetic.offworld.worldgen.Field
import dev.zoenetic.offworld.worldgen.FieldMeta

class ConstantField(private val value: Double) : Field {
    override val meta = FieldMeta(
        nativeResolution = Int.MAX_VALUE,
        interpolable = true,
        columnConstant = true,
    )
    override fun sample(x: Double, y: Double, z: Double) = value
}