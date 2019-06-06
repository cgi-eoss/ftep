import com.moowork.gradle.node.task.NodeTask
import com.netflix.gradle.plugins.packaging.CopySpecEnhancement
import org.redline_rpm.header.Architecture.NOARCH
import org.redline_rpm.header.Os.LINUX
import org.redline_rpm.payload.Directive

plugins {
    id("nebula.ospackage")
    id("com.github.node-gradle.node")
}

node {
    download = true
}

val closureCompiler by configurations.creating

dependencies {
    closureCompiler("com.google.javascript:closure-compiler:v20190528")
}

val sourceDir = projectDir.resolve("src/main/resources")
val requireJsDir = projectDir.resolve("src/main/requireJs")
val testSourceDir = projectDir.resolve("src/test/resources")
val bannerVersion = if (project.version.toString().endsWith("-SNAPSHOT"))
    "${extra.get("rpm.version")}-dev" else
    "${extra.get("rpm.version")}"

val requireJs by tasks.creating(NodeTask::class) {
    setScript(file("${projectDir}/src/main/resources/app/scripts/vendor/requirejs/bin/r.js"))
    setArgs(listOf(
            "-o", file("${projectDir}/src/main/requireJs/build.js"),
            "out=${buildDir}/requireJs/app.js.full"
    ))
    inputs.file("${projectDir}/src/main/requireJs/build.js")
    inputs.files(fileTree("${projectDir}/src/main/resources/app/scripts") {
        include("**/*.js")
    })
    outputs.file("${buildDir}/requireJs/app.js.full")
}

val compressJS by tasks.creating(JavaExec::class) {
    classpath = closureCompiler
    main = "com.google.javascript.jscomp.CommandLineRunner"
    args = listOf(
            "--warning_level=QUIET",
            "--compilation_level=SIMPLE_OPTIMIZATIONS",
            "--js_output_file=${buildDir}/requireJs/app.js"
    ) + tasks["requireJs"].outputs.files.map { it.path }
    inputs.files(tasks["requireJs"])
    outputs.file("${buildDir}/requireJs/app.js")
}

val stageApp by tasks.creating(Sync::class) {
    into(buildDir.resolve("staging"))
    includeEmptyDirs = false

    from(sourceDir.resolve("app")) {
        exclude("scripts/**/*.js", "index.html")
    }
    from(sourceDir.resolve("app/scripts/vendor/codemirror")) {
        into("scripts/vendor/codemirror")
        include("**/*")
    }
    from(compressJS) {
        into("scripts")
        include("**/*")
    }
    from(sourceDir.resolve("app/scripts/ftepConfig.js")) {
        into("scripts")
    }
    from(sourceDir.resolve("app")) {
        include("index.html")
        filter {
            it.replace("<script data-main=\"scripts/main\" src=\"scripts/vendor/requirejs/require.js\"></script>",
                    "<script src=\"scripts/app.js\"></script>")
        }
        filter {
            it.replace("<meta id=\"version\" name=\"version\" content=\"dev\">",
                    "<meta id=\"version\" name=\"version\" content=\"${bannerVersion}\">")
        }
    }
}

ospackage {
    packageName = "f-tep-portal"
    version = extra.get("rpm.version") as String?
    release = extra.get("rpm.release") as String?
    setArch(NOARCH)
    os = LINUX

    user = "ftep"
    permissionGroup = "ftep"
}

tasks {
    "buildRpm"(com.netflix.gradle.plugins.rpm.Rpm::class) {
        into("/var/www/html/f-tep") {
            addParentDirs = false
            from(stageApp) {
                include("**")
                exclude("scripts/ftepConfig.js")
            }
            from(stageApp) {
                include("scripts/ftepConfig.js")
                CopySpecEnhancement.fileType(this, Directive(Directive.RPMFILE_CONFIG or Directive.RPMFILE_NOREPLACE))
            }
        }
    }
}

val pkg by configurations.creating
artifacts.add("pkg", tasks["buildRpm"])

sonarqube {
    properties {
        property("sonar.sources", "src/main/resources/app")
        property("sonar.exclusions", "src/main/resources/app/scripts/vendor/**/*")
    }
}
