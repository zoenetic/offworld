// root, config only, no code!

plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.fabric.loom) apply false
}

// Version is derived from `mod_version` (in gradle.properties) plus the
// commit-count + sha since the most recent `v*` tag. The tag name is only
// used to anchor the count; it does NOT contribute to the version string.
// That lets tag names carry descriptive suffixes (`v0.1.0-spine`,
// `v0.2.0-devloop`) without polluting the runtime version.
//
// Output shapes (with mod_version=0.1.0):
//   HEAD is exactly tag `v0.1.0`:               0.1.0
//   N commits past any `v*` tag:                0.1.0-dev.N+<sha>
//   Working tree dirty (uncommitted edits):     ...+<sha>.dirty
//   No `v*` tags exist yet:                     0.1.0+<sha>[.dirty]
//   No git context (release tarball):           0.1.0+nogit
//
// `git describe` runs at configuration time via providers.exec so the result
// is captured before any task executes — configuration cache stays happy.
val modVersion: String = run {
    val baseVersion = providers.gradleProperty("mod_version").get()

    val describeResult = providers.exec {
        // --long so we always get the N+sha suffix even when HEAD is exactly
        // on a tag — that gives us a uniform shape to match.
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
            // Exactly on a tag and clean → release version. Anything else is a dev build.
            if (count == "0" && dirty.isEmpty()) baseVersion
            else "$baseVersion-dev.$count+$sha$dirty"
        }

        shaOnlyShape.matches(describe) -> {
            // No `v*` tags exist anywhere reachable. Use sha alone as metadata.
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
