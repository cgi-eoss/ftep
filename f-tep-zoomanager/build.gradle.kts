import com.netflix.gradle.plugins.packaging.CopySpecEnhancement
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

val zooJars by configurations.creating

dependencies {
    implementation(project(":f-tep-logging"))
    implementation(project(":f-tep-rpc"))
    implementation("org.freemarker:freemarker")
    implementation("org.jooq:jool-java-8")
    implementation("org.springframework.boot:spring-boot")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-undertow")
    implementation("org.springframework.cloud:spring-cloud-starter-netflix-eureka-client")
    implementation("org.apache.logging.log4j:log4j-iostreams")

    runtimeOnly(project(":f-tep-zoolib"))
    runtimeOnly("org.jolokia:jolokia-core")

    testImplementation("com.google.jimfs:jimfs")
    testImplementation("org.springframework.boot:spring-boot-starter-test")

    zooJars(project(":f-tep-zoolib", "shadow")) { isTransitive = false }
}

ospackage {
    packageName = "f-tep-zoomanager"
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

        into("/var/f-tep/zoomanager") {
            from(project.tasks.get("bootJar")) {
                include("*-bin.jar")
                rename("f-tep-zoomanager-.*\\-bin.jar", "f-tep-zoomanager.jar")
                setFileMode(500)
            }

            from("src/ospackage/application.properties") {
                fileType(this, Directive(Directive.RPMFILE_CONFIG or Directive.RPMFILE_NOREPLACE))
            }
        }

        into("/var/www/cgi-bin/jars") {
            from(zooJars)
            rename("f-tep-zoolib-.*\\-all.jar", "f-tep-zoolib.jar")
            CopySpecEnhancement.addParentDirs(this, false)
        }

        into("/usr/lib/systemd/system") {
            from("src/ospackage/f-tep-zoomanager.service")
            addParentDirs(this, false)
        }

        link("/etc/init.d/f-tep-zoomanager", "/var/f-tep/zoomanager/f-tep-zoomanager.jar")
    }
}

val pkg by configurations.creating
artifacts.add("pkg", tasks["buildRpm"])
