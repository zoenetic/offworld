package dev.offworld.content

import java.lang.foreign.*
import java.lang.invoke.MethodHandle
import java.nio.file.Files
import java.nio.file.StandardCopyOption

object GenesisLibrary {
    private val arena: Arena = Arena.global()
    private val linker: Linker = Linker.nativeLinker()
    private val lookup: SymbolLookup = loadBundledLibrary()

    private val regionLenHandle: MethodHandle = downcall(
        "genesis_region_len",
        FunctionDescriptor.of(
            ValueLayout.JAVA_LONG,
            ValueLayout.JAVA_DOUBLE, ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG,
        ),
    )

    private val generateHandle: MethodHandle = downcall(
        "genesis_generate_solidity",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.JAVA_DOUBLE, ValueLayout.JAVA_DOUBLE, ValueLayout.JAVA_DOUBLE,
            ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG,
            ValueLayout.ADDRESS, ValueLayout.JAVA_LONG,
        ),
    )

    fun regionLen(spacing: Double, nx: Long, nz: Long): Long =
        regionLenHandle.invokeWithArguments(spacing, nx, nz) as Long

    fun generateSolidity(
        minX: Double, minZ: Double, spacing: Double,
        nx: Long, nz: Long, out: MemorySegment, outLen: Long,
    ): Int = generateHandle.invokeWithArguments(minX, minZ, spacing, nx, nz, out, outLen) as Int

    private fun downcall(symbol: String, desc: FunctionDescriptor): MethodHandle =
        linker.downcallHandle(
            lookup.find(symbol).orElseThrow { IllegalStateException("missing symbol: $symbol") },
            desc,
        )

    private fun loadBundledLibrary(): SymbolLookup {
        val (dir, lib) = platform()
        val resource = "/native/$dir/$lib"
        val stream = GenesisLibrary::class.java.getResourceAsStream(resource)
            ?: error("bundled native library not found on classpath: $resource")
        val tmp = Files.createTempFile("genesis-", "-$lib")
        tmp.toFile().deleteOnExit()
        stream.use { Files.copy(it, tmp, StandardCopyOption.REPLACE_EXISTING) }
        return SymbolLookup.libraryLookup(tmp, arena)
    }

    private fun platform(): Pair<String, String> {
        val os = System.getProperty("os.name").lowercase()
        val arch = System.getProperty("os.arch").lowercase()
        val a = if ("aarch64" in arch || "arm64" in arch) "aarch64" else "x86-64"
        return when {
            "linux" in os -> "linux-$a" to "libgenesis.so"
            "mac" in os   -> "macos-$a" to "libgenesis.dylib"
            "win" in os   -> "windows-$a" to "genesis.dll"
            else -> error("unsupported OS: $os")
        }
    }
}