import com.netflix.gradle.plugins.packaging.CopySpecEnhancement
import com.netflix.gradle.plugins.rpm.Rpm
import com.sun.imageio.plugins.jpeg.JPEG.vendor
import org.redline_rpm.header.Architecture
import org.redline_rpm.header.Os
import org.redline_rpm.payload.Directive

plugins {
    base
    id("nebula.ospackage")
}

version = "1.6.0"

val buildZoo by tasks.creating(Exec::class) {
    executable = "${projectDir}/build.sh"
}

ospackage {
    packageName = "zoo-kernel"
    version = "1.6.0"
    release = "1"
    setArch(Architecture.X86_64)
    os = Os.LINUX
    packageDescription = "ZOO-Kernel WPS server"
    url = "http://zoo-project.org/"
    license = "MIT License"
    vendor = "F-TEP"
}

tasks {
    "buildRpm"(Rpm::class) {
        requires("java-1.8.0-openjdk-headless")
        requires("gdal")
        requires("fcgi")

        postInstall(file("postinst.sh"))

        into("/usr") {
            into("include/zoo") {
                from("${buildDir}/staging/usr/include/zoo")
                CopySpecEnhancement.addParentDirs(this, false)
            }
            into("lib") {
                from("${buildDir}/staging/usr/lib")
            }
        }

        into("/var/www/cgi-bin") {
            from("${buildDir}/staging/var/www/cgi-bin")
            CopySpecEnhancement.addParentDirs(this, false)

            into("sql") {
                from("src/zoo-project/zoo-kernel/sql/schema.sql") {
                    rename("schema.sql", "V1_6_0__Schema_install.sql")
                    CopySpecEnhancement.addParentDirs(this, false)
                }
            }
        }
    }
}

val pkg by configurations.creating
artifacts.add("pkg", tasks["buildRpm"])

sonarqube {
    setSkipProject(true)
}
