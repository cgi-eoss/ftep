buildscript {
    repositories {
        maven(url = project.property("gradlePluginPortalUrl")!!)
    }
}

plugins {
    id("com.github.ben-manes.versions") version "0.36.0"
    id("io.spring.dependency-management") version "1.0.11.RELEASE"

    // reporting
    jacoco
    id("org.sonarqube") version "2.7.1"

    // plugin management
    id("com.github.johnrengelman.shadow") version "6.1.0" apply false
    id("com.github.node-gradle.node") version "3.0.1" apply false
    id("com.google.protobuf") version "0.8.14" apply false
    id("io.freefair.lombok") version "5.3.0" apply false
    id("nebula.ospackage") version "8.5.0" apply false
    id("org.springframework.boot") version "2.1.6.RELEASE" apply false
}

allprojects {
    group = "com.cgi.eoss.f-tep"
    version = "3.1.3"

    buildscript {
        repositories {
            maven(url = project.property("gradlePluginPortalUrl")!!)
        }
    }

    repositories {
        maven(url = project.property("mavenCentralUrl")!!)
        maven(url = project.property("mavenGeosolutionsUrl")!!)
        maven(url = project.property("mavenOsgeoUrl")!!)
    }

    plugins.withType(JavaPlugin::class) {
        configure<JavaPluginConvention> {
            sourceCompatibility = JavaVersion.VERSION_1_8
        }
    }

    extra.set("spring-cloud.version", "Greenwich.SR1")
    extra.set("spring-boot.version", "2.1.6.RELEASE")

    extra.set("awaitility.version", "2.0.0")
    extra.set("commons-io.version", "2.6")
    extra.set("commons-lang3.version", "3.8.1")
    extra.set("commons-net.version", "3.6")
    extra.set("commons-text.version", "1.6")
    extra.set("disruptor.version", "3.4.2")
    extra.set("docker-java.version", "3.2.7")
    extra.set("embedded-database-spring-test.version", "1.3.1")
    extra.set("embedded-postgres-binaries-bom.version", "10.5.0")
    extra.set("failsafe.version", "2.4.0")
    extra.set("geojson-jackson.version", "1.8.1")
    extra.set("geoserver-manager.version", "1.7.0")
    extra.set("geotools.version", "17.2")
    extra.set("grpc-spring-boot-starter.version", "3.3.0")
    extra.set("grpc.version", "1.21.0")
    extra.set("guava.version", "25.1-jre")
    extra.set("hamcrest-junit.version", "2.0.0.0")
    extra.set("jclouds.version", "2.1.2")
    extra.set("jersey-core.version", "1.19.4")
    extra.set("jimfs.version", "1.1")
    extra.set("jool.version", "0.9.14")
    extra.set("logf42-gelf.version", "1.3.1")
    extra.set("lombok.version", "1.18.8")
    extra.set("mockftpserver.version", "2.7.1")
    extra.set("okhttp3.version", "3.14.2")
    extra.set("poi.version", "3.17")
    extra.set("protobuf-java.version", "3.7.1")
    extra.set("protoc.version", "3.7.1")
    extra.set("querydsl.version", "4.2.1")
    extra.set("sshj.version", "0.23.0")

    apply(plugin = "io.spring.dependency-management")

    configurations.all {
            resolutionStrategy.eachDependency {
                if (requested.group == "javax.media" && requested.name == "jai_core" && requested.version == "1.1.3") {
                    useTarget("${requested.group}:jai-core:${requested.version}")
                    because("\"javax.media:jai_core:1.1.3\" is not available in Maven Central")
            }
        }
        exclude(group = "org.springframework.boot", module = "spring-boot-starter-logging")
        exclude(group = "org.springframework.boot", module = "spring-boot-starter-tomcat")
    }

    dependencyManagement {
        imports {
            mavenBom("io.zonky.test.postgres:embedded-postgres-binaries-bom:${extra.get("embedded-postgres-binaries-bom.version")}")
            mavenBom("org.springframework.boot:spring-boot-dependencies:${extra.get("spring-boot.version")}")
            mavenBom("org.springframework.cloud:spring-cloud-dependencies:${extra.get("spring-cloud.version")}")
        }
        dependencies {
            dependency("com.github.docker-java:docker-java-core:${extra.get("docker-java.version")}")
            dependency("com.github.docker-java:docker-java-transport-zerodep:${extra.get("docker-java.version")}")
            dependency("com.google.guava:guava:${extra.get("guava.version")}")
            dependency("com.google.jimfs:jimfs:${extra.get("jimfs.version")}")
            dependency("com.google.protobuf:protobuf-java-util:${extra.get("protobuf-java.version")}")
            dependency("com.google.protobuf:protobuf-java:${extra.get("protobuf-java.version")}")
            dependency("com.hierynomus:sshj:${extra.get("sshj.version")}")
            dependency("com.lmax:disruptor:${extra.get("disruptor.version")}")
            dependency("com.squareup.okhttp3:logging-interceptor:${extra.get("okhttp3.version")}")
            dependency("com.squareup.okhttp3:mockwebserver:${extra.get("okhttp3.version")}")
            dependency("com.squareup.okhttp3:okhttp:${extra.get("okhttp3.version")}")
            dependency("commons-io:commons-io:${extra.get("commons-io.version")}")
            dependency("commons-net:commons-net:${extra.get("commons-net.version")}")
            dependency("de.grundid.opendatalab:geojson-jackson:${extra.get("geojson-jackson.version")}")
            dependency("io.github.lognet:grpc-spring-boot-starter:${extra.get("grpc-spring-boot-starter.version")}")
            dependency("io.grpc:grpc-netty:${extra.get("grpc.version")}")
            dependency("io.grpc:grpc-protobuf:${extra.get("grpc.version")}")
            dependency("io.grpc:grpc-stub:${extra.get("grpc.version")}")
            dependency("io.grpc:grpc-testing:${extra.get("grpc.version")}")
            dependency("io.zonky.test:embedded-database-spring-test:${extra.get("embedded-database-spring-test.version")}")
            dependency("it.geosolutions:geoserver-manager:${extra.get("geoserver-manager.version")}")
            dependency("net.jodah:failsafe:${extra.get("failsafe.version")}")
            dependency("org.apache.commons:commons-lang3:${extra.get("commons-lang3.version")}")
            dependency("org.apache.commons:commons-text:${extra.get("commons-text.version")}")
            dependency("org.apache.jclouds.api:openstack-cinder:${extra.get("jclouds.version")}")
            dependency("org.apache.jclouds.api:openstack-keystone:${extra.get("jclouds.version")}")
            dependency("org.apache.jclouds.api:openstack-neutron:${extra.get("jclouds.version")}")
            dependency("org.apache.jclouds.api:openstack-nova:${extra.get("jclouds.version")}")
            dependency("org.apache.jclouds.driver:jclouds-okhttp:${extra.get("jclouds.version")}")
            dependency("org.apache.jclouds.driver:jclouds-slf4j:${extra.get("jclouds.version")}")
            dependency("org.apache.jclouds.driver:jclouds-sshj:${extra.get("jclouds.version")}")
            dependency("org.apache.jclouds:jclouds-core:${extra.get("jclouds.version")}")
            dependency("org.apache.poi:poi:${extra.get("poi.version")}")
            dependency("org.awaitility:awaitility:${extra.get("awaitility.version")}")
            dependency("org.geotools:gt-epsg-extension:${extra.get("geotools.version")}")
            dependency("org.geotools:gt-epsg-hsql:${extra.get("geotools.version")}")
            dependency("org.geotools:gt-geojson:${extra.get("geotools.version")}")
            dependency("org.geotools:gt-geometry:${extra.get("geotools.version")}")
            dependency("org.geotools:gt-geotiff:${extra.get("geotools.version")}")
            dependency("org.geotools:gt-main:${extra.get("geotools.version")}")
            dependency("org.geotools:gt-referencing:${extra.get("geotools.version")}")
            dependency("org.geotools:gt-shapefile:${extra.get("geotools.version")}")
            dependency("org.geotools:gt-xml:${extra.get("geotools.version")}")
            dependency("org.graylog2.log4j2:log4j2-gelf:${extra.get("logf42-gelf.version")}")
            dependency("org.hamcrest:hamcrest-junit:${extra.get("hamcrest-junit.version")}")
            dependency("org.jooq:jool-java-8:${extra.get("jool.version")}")
            dependency("org.mockftpserver:MockFtpServer:${extra.get("mockftpserver.version")}")
        }
    }

    plugins.withType(io.freefair.gradle.plugins.lombok.LombokPlugin::class) {
        configure<io.freefair.gradle.plugins.lombok.LombokExtension> {
            setVersion(property("lombok.version")!!)
            getConfig().put("lombok.log.fieldName", "LOG")
        }
    }

    tasks.withType(com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar::class) {
        classifier = "all"
    }

    tasks.withType(org.springframework.boot.gradle.tasks.bundling.BootJar::class) {
        classifier = "bin"
        launchScript()
    }

    val snapshotRpmQualifier = System.getenv("BUILD_NUMBER")
        ?: java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
    extra.set("rpm.version", if (project.version.toString().endsWith("-SNAPSHOT")) project.version.toString().split("-")[0] else version)
    extra.set("rpm.release", if (project.version.toString().endsWith("-SNAPSHOT")) "SNAPSHOT${snapshotRpmQualifier}" else "1")

    sonarqube {
        properties {
            property("sonar.jacoco.reportPaths", "${project.buildDir}/jacoco/test.exec, ${rootProject.buildDir}/jacoco/aggregateCoverage.exec")
        }
    }
}

val aggregateCoverage by tasks.creating(JacocoMerge::class) {
    executionData = files(subprojects.mapNotNull {
        val coverageFileLocation = it.buildDir.resolve("jacoco/test.exec")
        if (coverageFileLocation.exists()) coverageFileLocation else null
    })
}

tasks["sonarqube"].dependsOn(aggregateCoverage)
