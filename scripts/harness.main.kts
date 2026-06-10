import java.lang.foreign.*
import java.lang.invoke.MethodHandle
import java.nio.file.Path
import java.io.BufferedOutputStream
import java.io.FileOutputStream

val libPath = args.getOrElse(0) { "target/debug/libgenesis.so" }

val minX = 0.0; val minZ = 0.0; val spacing = 1.0;
val nx = 256L; val nz = 1L

Arena.ofConfined().use { arena ->
    val lib = SymbolLookup.libraryLookup(Path.of(libPath), arena)
    val linker = Linker.nativeLinker()

    val regionLen: MethodHandle = linker.downcallHandle(
        lib.find("genesis_region_len").orElseThrow(),
        FunctionDescriptor.of(
            ValueLayout.JAVA_LONG,
            ValueLayout.JAVA_DOUBLE, ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG,
        ),
    )

    val generate: MethodHandle = linker.downcallHandle(
        lib.find("genesis_generate_solidity").orElseThrow(),
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.JAVA_DOUBLE, ValueLayout.JAVA_DOUBLE, ValueLayout.JAVA_DOUBLE,
                ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG,
                ValueLayout.ADDRESS, ValueLayout.JAVA_LONG,
        ),
    )

    val len = regionLen.invokeWithArguments(spacing, nx, nz) as Long
    val out = arena.allocate(len * 4)

    val rc = generate.invokeWithArguments(minX, minZ, spacing, nx, nz, out, len) as Int
    require(rc == 0) { "generate failed, rc=$rc" }

    val ny = len / (nx * nz)
    writePgm("harness.pgm", out, nx.toInt(), ny.toInt())
    println("wrote harness.pgm (${nx}x${ny})")
}

fun writePgm(path: String, buf: MemorySegment, nx: Int, ny: Int) {
    val pixels = ByteArray(nx * ny)
    for (j in 0 until ny) {
        val row = ny - 1 - j
        for (i in 0 until nx) {
            val index = j.toLong() * nx + i
            val s = buf.getAtIndex(ValueLayout.JAVA_FLOAT, index)
            pixels[row * nx + i] = (s.coerceIn(0f, 1f) * 255f).toInt().toByte()
        }
    }
    BufferedOutputStream(FileOutputStream(path)).use { os ->
        os.write("P5\n$nx $ny\n255\n".toByteArray(Charsets.US_ASCII))
        os.write(pixels)
    }
}