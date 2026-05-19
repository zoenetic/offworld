package dev.zoenetic.offworld

@JvmInline
value class BlockKey(val id: String) {
    val namespace: String get() = id.substringBefore(":", "minecraft")
    val path: String get() = id.substringAfter(":")
    override fun toString(): String = id
    companion object {
        fun offworld(path: String) = BlockKey("offworld:$path")
        fun vanilla(path: String) = BlockKey("minecraft:$path")
    }
}

@JvmInline
value class BiomeKey(val id: String) {
    val namespace: String get() = id.substringBefore(":", "minecraft")
    val path: String get() = id.substringAfter(":")
    override fun toString(): String = id
    companion object {
        fun offworld(path: String) = BiomeKey("offworld:$path")
        fun vanilla(path: String) = BiomeKey("minecraft:$path")
    }
}