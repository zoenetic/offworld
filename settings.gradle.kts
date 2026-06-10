rootProject.name = "offworld"

val offworldModules = listOf(
    "content",
    "menu",
    "tech",
    "fabric:menu",
    "fabric:tech",
    "neoforge:menu",
    "neoforge:tech",
)

include(":offworld")
project(":offworld").projectDir = file("minecraft/offworld")

for (m in offworldModules) {
    val path = ":offworld:$m"
    include(path)
    project(path).projectDir = file("minecraft/" + path.removePrefix(":").replace(':', '/'))
}
