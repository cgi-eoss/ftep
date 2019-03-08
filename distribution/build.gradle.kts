plugins {
    base
}

val packages by configurations.creating
val puppetModules by configurations.creating

dependencies {
    packages(project(path = ":f-tep-portal", configuration = "pkg"))
    packages(project(path = ":f-tep-server", configuration = "pkg"))
    packages(project(path = ":f-tep-serviceregistry", configuration = "pkg"))
    packages(project(path = ":f-tep-worker", configuration = "pkg"))
    packages(project(path = ":f-tep-zoomanager", configuration = "pkg"))

    packages(project(path = ":pkg", configuration = "pkg"))
    packages(project(path = ":resto", configuration = "pkg"))

    puppetModules(project(path = ":puppet", configuration = "allModules"))
}

val distPackages by tasks.creating(Sync::class) {
    dependsOn(packages)

    into(buildDir.resolve("repo"))
    into("6/local/noarch/RPMS") {
        from(packages)
        include("**/*.noarch.rpm")
    }
    into("6/local/x86_64/RPMS") {
        from(packages)
        include("**/*.x86_64.rpm")
    }
}

val distPuppet by tasks.creating(Sync::class) {
    dependsOn(puppetModules)

    into(buildDir.resolve("puppet"))
    from(projectDir.resolve("puppet"))

    into("local-modules") {
        from(puppetModules.map { tarTree(it) })
    }
}

val buildDist by tasks.creating(Sync::class) {
    into("${rootDir}/.dist")

    preserve {
        include("puppet/modules/**")
        include("repo/repodata/**")
    }

    into("repo") {
        from(distPackages)
    }
    into("puppet") {
        from(distPuppet)
    }
}

sonarqube {
    setSkipProject(true)
}
