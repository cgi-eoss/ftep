plugins {
    base
}

val pkg by configurations.creating
fileTree(mapOf("dir" to ".", "include" to listOf("**/*.rpm"))).forEach {
    artifacts.add("pkg", it)
}

sonarqube {
    setSkipProject(true)
}
