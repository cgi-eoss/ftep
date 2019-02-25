import org.gradle.internal.hash.HashUtil

plugins {
    base
}

group = "com.cgi.eoss.f-tep"
version = "0.1.0"

val moduleTars by configurations.creating
val allModules by configurations.creating

// Puppet modules to stage into ${buildDir}/modules and package into ${buildDir}/distributions
val puppetModules = listOf("cgieoss-ftep")
val moduleMetadata = puppetModules.map { it to groovy.json.JsonSlurper().parse(file("${it}/metadata.json")) }.toMap()

val sourcesTasks = puppetModules.map { module ->
    module to tasks.create("collectSources_$module", Sync::class) {
        val moduleShortName = (moduleMetadata[module] as Map<String, Any?>)["name"] as String
        val targetDir = buildDir.resolve("modules/${moduleShortName}")

        val srcs = fileTree(mapOf("dir" to module, "include" to listOf("**/*"), "exclude" to listOf(".*")))

        var checksumsFile = projectDir.resolve("${module}/checksums.json")
        if (!srcs.contains(checksumsFile)) {
            checksumsFile = temporaryDir.resolve("checksums.json")
            dependsOn(genChecksumsTask(module, checksumsFile, srcs))
        }

        from(srcs)
        from(checksumsFile)
        into(targetDir)
    }
}.toMap()

val buildModuleTasks = puppetModules.map { module ->
    module to tasks.create("buildModule_$module", Tar::class) {
        baseName = ((moduleMetadata[module] as Map<String, Any?>)["name"] as String).replace("/", "-")
        version = (moduleMetadata[module] as Map<String, Any?>)["version"] as String
        compression = Compression.GZIP
        extension = "tar.gz"
        from(sourcesTasks[module])
        into("${baseName}-${version}")
    }
}.toMap()

val allModulesTar by tasks.creating(Tar::class) {
    baseName = "allModules"
    compression = Compression.GZIP
    extension = "tar.gz"

    puppetModules.forEach {
        into(((moduleMetadata[it] as Map<String, Any?>)["name"] as String).split("-", "/")[1]) {
            from(sourcesTasks[it])
        }
    }
}

fun genChecksumsTask(module: String, checksumsFile: File, srcs: FileTree): Task {
    return tasks.create("genChecksums_${module}", SourceTask::class) {
        mustRunAfter(tasks["clean"])

        source(srcs)
        outputs.dir(checksumsFile.parent)
        outputs.file(checksumsFile)

        doLast {
            val checksums = mutableMapOf<String, String>()
            srcs.visit {
                if (!isDirectory) {
                    val hash = HashUtil.createHash(this.file, "MD5").asZeroPaddedHexString(32)
                    checksums.put(this.relativePath.toString(), hash)
                }
            }
            checksumsFile.parentFile.mkdirs()
            checksumsFile.writeText(groovy.json.JsonOutput.prettyPrint(groovy.json.JsonOutput.toJson(checksums.toSortedMap())))
        }
    }
}

artifacts {
    add(allModules.name, allModulesTar)
    buildModuleTasks.values.forEach {
        add(moduleTars.name, it)
    }
}

sonarqube {
    setSkipProject(true)
}
