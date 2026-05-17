plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.fabric.loom) apply false
}

val modVersion: String = run {
    val baseVersion = providers.gradleProperty("mod_version").get()

    val describeResult = providers.exec {
        commandLine(
            "git", "describe",
            "--tags", "--long", "--always",
            "--dirty=.dirty",
            "--match", "v*",
        )
        isIgnoreExitValue = true
    }
    val describe = describeResult.standardOutput.asText.map { it.trim() }.orNull
    val exitCode = describeResult.result.map { it.exitValue }.orNull

    val tagShape = Regex("""^v.+-(\d+)-g([0-9a-f]+)(\.dirty)?$""")
    val shaOnlyShape = Regex("""^([0-9a-f]+)(\.dirty)?$""")

    when {
        describe.isNullOrEmpty() || exitCode != 0 -> "$baseVersion+nogit"

        tagShape.matches(describe) -> {
            val (count, sha, dirty) = tagShape.find(describe)!!.destructured
            if (count == "0" && dirty.isEmpty()) baseVersion
            else "$baseVersion-dev.$count+$sha$dirty"
        }

        shaOnlyShape.matches(describe) -> {
            val (sha, dirty) = shaOnlyShape.find(describe)!!.destructured
            "$baseVersion+$sha$dirty"
        }

        else -> "$baseVersion+unknown.$describe"
    }
}

allprojects {
    group = "dev.zoenetic"
    version = modVersion
}
