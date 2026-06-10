package dev.offworld.content

import org.junit.jupiter.api.Test
import java.lang.foreign.Arena
import java.lang.foreign.ValueLayout
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GenesisLibraryTest {
    @Test
    fun generatesDemoTerrainAcrossTheSeam() {
        val spacing = 1.0
        val nx = 4L
        val nz = 1L

        Arena.ofConfined().use { arena ->
            val len = GenesisLibrary.regionLen(spacing, nx, nz)
            assertTrue(len > 0, "region length must be positive")

            val out = arena.allocate(len * 4)
            val rc = GenesisLibrary.generateSolidity(0.0, 0.0, spacing, nx, nz, out, len)
            assertEquals(0, rc, "generate should succeed")

            val ny = (len / (nx * nz)).toInt()
            val column = (0 until ny).map { j ->
                out.getAtIndex(ValueLayout.JAVA_FLOAT, j.toLong() * nx)
            }
            assertTrue(column.any { it == 1.0f }, "column should contain solid")
            assertEquals(0.0f, column.last(), "top of the world should be air")
        }
    }
}