import com.netflix.gradle.plugins.packaging.CopySpecEnhancement
import org.redline_rpm.header.Architecture
import org.redline_rpm.header.Os
import org.redline_rpm.payload.Directive

plugins {
    base
    id("nebula.ospackage")
}

version = "2.4"

ospackage {
    packageName = "resto"
    version = "2.4"
    release = "7"
    setArch(Architecture.X86_64)
    os = Os.LINUX
    packageDescription = "Resto Earth Observation products search engine (with F-TEP modifications)"
    url = "https://github.com/jjrom/resto"
    license = "Apache License 2.0"

    user = "root"
}

tasks {
    "buildRpm"(com.netflix.gradle.plugins.rpm.Rpm::class) {
        into("/opt/resto") {
            from(".") {
                include(".htaccess")
                include("robots.txt")
                include("favicon.ico")
                include("index.php")
                include("include/**")
                include("lib/**")
                include("vendor/**")
                include("_examples/**")
                include("_flyway_migration/**")
                include("_install/**")
                include("_scripts/**")
                exclude("lib/config.php")
            }

            from(".") {
                include("include/config.php")
                CopySpecEnhancement.fileType(this, Directive(Directive.RPMFILE_CONFIG or Directive.RPMFILE_NOREPLACE))
            }
        }
    }
}

val pkg by configurations.creating
artifacts.add("pkg", tasks["buildRpm"])

sonarqube {
    setSkipProject(true)
}
