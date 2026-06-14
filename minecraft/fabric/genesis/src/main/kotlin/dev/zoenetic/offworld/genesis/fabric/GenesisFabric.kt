package dev.zoenetic.offworld.genesis.fabric

import dev.offworld.content.GenesisLibrary
import net.fabricmc.api.ModInitializer
import org.slf4j.LoggerFactory
import java.lang.foreign.Arena
import java.lang.foreign.ValueLayout

object GenesisFabric : ModInitializer {
    private val log = LoggerFactory.getLogger("offworld-genesis")

    override fun onInitialize() {
        val spacing = 1.0
        val nx = 4L
        val nz = 1L

        Arena.ofConfined().use { arena ->
            val len = GenesisLibrary.regionLen(spacing, nx, nz)
            val solidity = arena.allocate(ValueLayout.JAVA_FLOAT, len)
            val material = arena.allocate(ValueLayout.JAVA_SHORT, len)

            val rc = GenesisLibrary.generate(0.0, 0.0, spacing, nx, nz, solidity, material, len)
            val floor = material.getAtIndex(ValueLayout.JAVA_SHORT, 0).toInt()

            log.info("genesis online: rc={}, region cells={}, floor material={}", rc, len, floor)
        }
    }
}