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
    implementation(project(":f-tep-api"))
    implementation(project(":f-tep-logging"))
    implementation(project(":f-tep-orchestrator"))
    implementation("org.springframework.boot:spring-boot")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-undertow")

    runtimeOnly("org.hsqldb:hsqldb")
    runtimeOnly("org.postgresql:postgresql")

    testImplementation(project(":f-tep-clouds"))
    testImplementation(project(":f-tep-worker"))
    testImplementation(project(":f-tep-io"))
    testImplementation(project(":f-tep-persistence"))
    testImplementation(project(":f-tep-catalogue"))
    testImplementation("org.springframework.boot:spring-boot-starter-test")

    testRuntimeOnly("org.apache.activemq:activemq-kahadb-store")
}

ospackage {
    packageName = "f-tep-server"
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

        into("/var/f-tep/server") {
            from(project.tasks.get("bootJar")) {
                rename("f-tep-server-.*\\-bin.jar", "f-tep-server.jar")
                setFileMode(500)
            }

            from("src/ospackage/application.properties") {
                fileType(this, Directive(Directive.RPMFILE_CONFIG or Directive.RPMFILE_NOREPLACE))
            }
        }

        into("/usr/lib/systemd/system") {
            from("src/ospackage/f-tep-server.service")
            addParentDirs(this, false)
        }

        link("/etc/init.d/f-tep-server", "/var/f-tep/server/f-tep-server.jar")
    }
}

val pkg by configurations.creating
artifacts.add("pkg", tasks["buildRpm"])
