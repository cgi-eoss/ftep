import com.google.protobuf.gradle.*

//
plugins {
    `java-library`
    jacoco
    id("io.freefair.lombok")
    id("com.google.protobuf")
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:${property("protoc.version")}"
    }
    plugins {
        id("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:${property("grpc.version")}"
        }
    }
    generateProtoTasks {
        ofSourceSet("main").forEach {
            it.plugins {
                id("grpc")
            }
        }
    }
}

dependencies {
    implementation(project(":f-tep-logging"))

    api(project(":f-tep-model"))
    api("io.github.lognet:grpc-spring-boot-starter")
    api("org.springframework.cloud:spring-cloud-commons")
}
