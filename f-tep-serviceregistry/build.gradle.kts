import com.netflix.gradle.plugins.packaging.CopySpecEnhancement.addParentDirs
import com.netflix.gradle.plugins.packaging.CopySpecEnhancement.fileType
import org.redline_rpm.header.Architecture.NOARCH
import org.redline_rpm.header.Os.LINUX
import org.redline_rpm.payload.Directive

plugins {
    java
    jacoco
    id("io.freefair.lombok")
    id("org.springframework.boot")
    id("nebula.ospackage")
}

dependencies {
    implementation(project(":f-tep-logging"))
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-undertow")
    implementation("org.springframework.cloud:spring-cloud-starter-netflix-eureka-server")
}

ospackage {
    packageName = "f-tep-serviceregistry"
    version = extra.get("rpm.version") as String?
    release = extra.get("rpm.release") as String?
    setArch(NOARCH)
    os = LINUX

    user = "ftep"
    permissionGroup = "ftep"
}

tasks {
    "buildRpm"(com.netflix.gradle.plugins.rpm.Rpm::class) {
        preInstall(file("src/ospackage/preinst.sh"))
        postInstall(file("src/ospackage/postinst.sh"))
        preUninstall(file("src/ospackage/prerm.sh"))

        into("/var/f-tep/serviceregistry") {
            from(project.tasks.get("bootJar")) {
                rename("f-tep-serviceregistry-.*\\-bin.jar", "f-tep-serviceregistry.jar")
                setFileMode(500)
            }

            from("src/ospackage/application.properties") {
                fileType(this, Directive(Directive.RPMFILE_CONFIG or Directive.RPMFILE_NOREPLACE))
            }
        }

        into("/usr/lib/systemd/system") {
            from("src/ospackage/f-tep-serviceregistry.service")
            addParentDirs(this, false)
        }

        link("/etc/init.d/f-tep-serviceregistry", "/var/f-tep/serviceregistry/f-tep-serviceregistry.jar")
    }
}

val pkg by configurations.creating
artifacts.add("pkg", tasks["buildRpm"])
