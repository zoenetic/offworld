package dev.zoenetic.offworld

import dev.zoenetic.offworld.block.OffworldBlocks
import dev.zoenetic.offworld.block.OffworldCreativeTab
import dev.zoenetic.offworld.fabric.worldgen.OffworldChunkGenerator
import net.fabricmc.api.ModInitializer
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object OffworldMod : ModInitializer {
    const val MOD_ID = "offworld"
    val LOGGER: Logger = LoggerFactory.getLogger(MOD_ID)
    override fun onInitialize() {
        LOGGER.info("Offworld initializing...")
        OffworldBlocks.register(MOD_ID)
        OffworldCreativeTab.init()
        Registry.register(
            BuiltInRegistries.CHUNK_GENERATOR,
            Identifier.fromNamespaceAndPath(MOD_ID, "offworld"),
            OffworldChunkGenerator.CODEC,
        )
    }
}