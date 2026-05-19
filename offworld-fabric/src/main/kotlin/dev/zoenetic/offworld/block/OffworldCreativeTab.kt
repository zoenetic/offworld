package dev.zoenetic.offworld.block

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents
import net.minecraft.world.item.CreativeModeTabs

object OffworldCreativeTab {
    fun init() {
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.NATURAL_BLOCKS)
            .register { output ->
                OffworldBlocks.byName.values.forEach { output.accept(it) }
            }
    }
}