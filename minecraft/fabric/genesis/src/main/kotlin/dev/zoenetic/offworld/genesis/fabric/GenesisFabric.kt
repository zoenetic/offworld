package dev.zoenetic.offworld.genesis.fabric

import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.IntegerArgumentType.integer
import com.mojang.brigadier.context.CommandContext
import dev.offworld.content.GenesisLibrary
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState
import org.slf4j.LoggerFactory
import java.lang.foreign.Arena
import java.lang.foreign.ValueLayout

object GenesisFabric : ModInitializer {
    private val log = LoggerFactory.getLogger("offworld-genesis")

    override fun onInitialize() {
        CommandRegistrationCallback.EVENT.register { dispatcher, _, _, ->
            dispatcher.register(
                Commands.literal("genesis")
                    .executes { ctx -> place(ctx, 16) }
                    .then(
                        Commands.argument("size", integer(1, 1024))
                            .executes { ctx -> place(ctx, IntegerArgumentType.getInteger(ctx, "size")) }
                    )
            )
        }
        log.info("offworld-genesis: /genesis command registered")
    }

    private fun blockFor(material: Int): BlockState = when (material) {
        1 -> Blocks.BEDROCK.defaultBlockState()    // bedrock
        2 -> Blocks.CALCITE.defaultBlockState()    // limestone
        3 -> Blocks.DEEPSLATE.defaultBlockState()  // shale
        4 -> Blocks.SANDSTONE.defaultBlockState()  // sandstone
        5 -> Blocks.STONE.defaultBlockState()      // stone
        6 -> Blocks.GRAVEL.defaultBlockState()     // scree
        7 -> Blocks.DIRT.defaultBlockState()       // soil
        8 -> Blocks.SAND.defaultBlockState()       // sand
        9 -> Blocks.SNOW_BLOCK.defaultBlockState() // snow
        else -> Blocks.AIR.defaultBlockState()
    }

    private fun place(ctx: CommandContext<CommandSourceStack>, size: Int): Int {
        val source = ctx.source
        val level = source.level
        val origin = BlockPos.containing(source.position)

        val spacing = 1.0
        val nx = size.toLong()
        val nz = size.toLong()

        Arena.ofConfined().use { arena ->
            val len = GenesisLibrary.regionLen(spacing, nx, nz)
            val solidity =  arena.allocate(ValueLayout.JAVA_FLOAT, len)
            val material = arena.allocate(ValueLayout.JAVA_SHORT, len)

            val rc = GenesisLibrary.generate(
                origin.x.toDouble(), origin.z.toDouble(),
                spacing, nx, nz, solidity, material, len
            )
            if (rc != 0) {
                source.sendFailure(Component.literal("genesis: generate failed (rc=$rc)"))
                return 0
            }

            val ny = (len / (nx * nz)).toInt()
            var placed = 0
            for (k in 0 until nz.toInt()) {
                for (i in 0 until nx.toInt()) {
                    for (j in 0 until ny) {
                        val idx = (k.toLong() * ny + j) * nx + i
                        if (solidity.getAtIndex(ValueLayout.JAVA_FLOAT, idx) < 0.5f) continue
                        val mat = material.getAtIndex(ValueLayout.JAVA_SHORT, idx).toInt()
                        level.setBlock(origin.offset(i, j, k), blockFor(mat), 3)
                        placed++
                    }
                }
            }
            source.sendSuccess({ Component.literal("genesis: placed $placed blocks") }, false)
        }
        return 1
    }
}