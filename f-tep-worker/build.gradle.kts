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
    implementation(project(":f-tep-clouds"))
    implementation(project(":f-tep-logging"))
    implementation(project(":f-tep-rpc"))
    implementation(project(":f-tep-queues"))
    implementation(project(":f-tep-io"))
    implementation("com.github.docker-java:docker-java-core")
    implementation("com.github.docker-java:docker-java-transport-zerodep")
    implementation("org.springframework.boot:spring-boot")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-undertow")
    implementation("org.springframework.cloud:spring-cloud-starter-netflix-eureka-client")
    implementation("org.springframework.data:spring-data-jpa")
    implementation("org.springframework:spring-orm")
    implementation("org.jooq:jool-java-8")
    implementation("org.apache.commons:commons-lang3")

    runtimeOnly("org.jolokia:jolokia-core")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

ospackage {
    packageName = "f-tep-worker"
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

        into("/var/f-tep/worker") {
            from(project.tasks.get("bootJar")) {
                rename("f-tep-worker-.*\\-bin.jar", "f-tep-worker.jar")
                setFileMode(500)
            }

            from("src/ospackage/application.properties") {
                fileType(this, Directive(Directive.RPMFILE_CONFIG or Directive.RPMFILE_NOREPLACE))
            }
        }

        into("/usr/lib/systemd/system") {
            from("src/ospackage/f-tep-worker.service")
            addParentDirs(this, false)
        }

        link("/etc/init.d/f-tep-worker", "/var/f-tep/worker/f-tep-worker.jar")
    }
    // Enabled for f-tep-server test import only
    "jar"(Jar::class) {
        enabled = true
    }
}

val pkg by configurations.creating
artifacts.add("pkg", tasks["buildRpm"])
