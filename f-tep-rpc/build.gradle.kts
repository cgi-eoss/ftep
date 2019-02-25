import com.google.protobuf.gradle.*

//
plugins {
    `java-library`
    jacoco
    id("io.franzbecker.gradle-lombok")
    id("com.google.protobuf")
}

protobuf {
    protoc {
        // The artifact spec for the Protobuf Compiler
        artifact = "com.google.protobuf:protoc:${extra.get("protoc.version")}"
    }
    plugins {
        // Optional: an artifact spec for a protoc plugin, with "grpc" as
        // the identifier, which can be referred to in the "plugins"
        // container of the "generateProtoTasks" closure.
        id("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:${extra.get("grpc.version")}"
        }
    }
    generateProtoTasks {
        ofSourceSet("main").forEach {
            it.plugins {
                // Apply the "grpc" plugin whose spec is defined above, without options.
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
