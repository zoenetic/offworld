package dev.zoenetic.offworld.block

import dev.zoenetic.offworld.BlockKey
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.Item
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.SoundType
import net.minecraft.world.level.block.state.BlockBehaviour

enum class BlockKind { CUBE_ALL, STALK }

class BlockDef internal constructor(
    val key: BlockKey,
    val hex: String,
    val kind: BlockKind,
    val toBlock: (ResourceKey<Block>) -> Block,
)

object OffworldBlocks {

    private val _defs: MutableMap<BlockKey, BlockDef> = LinkedHashMap()
    private val _registered: MutableMap<BlockKey, Block> = LinkedHashMap()

    val GRIST       = stoneLike("grist",       "#2E2A1E")
    val CHERT       = stoneLike("chert",       "#7A6A56")
    val OPAL        = stoneLike("opal",        "#C0C8C0", hardness = 2.0f, resistance = 8.0f)
    val QUARTZITE   = stoneLike("quartzite",   "#C8BAA0")
    val SILICA_SAND = sandLike ("silica_sand", "#E8E2D0")
    val GLASSGRASS  = stalk    ("glassgrass",  "#A8D4B8")

    val defs: List<BlockDef> get() = _defs.values.toList()

    fun get(key: BlockKey): Block = _registered[key]
        ?: error("Block $key not registered")

    val byName: Map<String, Block> get() = _registered.mapKeys { (k, ) -> k.id }
    val all: Collection<Block> get() = _registered.values

    fun register(modId: String) {
        check(_registered.isEmpty()) { "OffworldBlocks has already been registered" }
        for (def in _defs.values) {
            check(def.key.namespace == modId) {
                "block ${def.key} declared under namespace '${def.key.namespace}' but registering as '$modId'"
            }
            val id = Identifier.fromNamespaceAndPath(modId, def.key.path)
            val blockKey: ResourceKey<Block> = ResourceKey.create(Registries.BLOCK, id)
            val itemKey: ResourceKey<Item> = ResourceKey.create(Registries.ITEM, id)
            val block = def.toBlock(blockKey)
            Registry.register(BuiltInRegistries.BLOCK, blockKey, block)
            Registry.register(
                BuiltInRegistries.ITEM,
                itemKey,
                BlockItem(block, Item.Properties().useBlockDescriptionPrefix().setId(itemKey))
            )
            _registered[def.key] = block
        }
    }

    private fun stoneLike(
        name: String, hex: String,
        hardness: Float = 1.5f, resistance: Float = 6.0f,
    ): BlockKey = define(name, hex, BlockKind.CUBE_ALL) { key ->
        Block(
            BlockBehaviour.Properties.of()
                .strength(hardness, resistance)
                .sound(SoundType.STONE)
                .requiresCorrectToolForDrops()
                .setId(key)
        )
    }

    private fun sandLike(name: String, hex: String): BlockKey =
        define(name, hex, BlockKind.CUBE_ALL) { key ->
            Block(
                BlockBehaviour.Properties.of()
                    .strength(0.5f)
                    .sound(SoundType.SAND)
                    .setId(key)
            )
        }

    private fun stalk(name: String, hex: String): BlockKey =
        define(name, hex, BlockKind.STALK) { key ->
            Block(
                BlockBehaviour.Properties.of()
                    .strength(0.1f)
                    .noCollision()
                    .noOcclusion()
                    .sound(SoundType.AMETHYST)
                    .setId(key)
            )
        }

    private fun define(
        path: String, hex: String, kind: BlockKind,
        toBlock: (ResourceKey<Block>) -> Block,
    ): BlockKey {
        val key = BlockKey.offworld(path)
        require(key !in _defs) { "duplicate block definition: $path"}
        _defs[key] = BlockDef(key = key, hex = hex, kind = kind, toBlock = toBlock)
        return key
    }
}