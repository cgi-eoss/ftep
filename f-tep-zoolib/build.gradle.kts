plugins {
    `java-library`
    jacoco
    id("io.freefair.lombok")
    id("com.github.johnrengelman.shadow")
}

dependencies {
    implementation(project(":f-tep-logging"))
    implementation(project(":f-tep-rpc"))

    testImplementation("junit:junit")
    testImplementation("org.mockito:mockito-core")
    testImplementation("io.grpc:grpc-testing")
}

tasks {
    "shadowJar"(com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar::class) {
        transform(com.github.jengelman.gradle.plugins.shadow.transformers.Log4j2PluginsCacheFileTransformer::class.java)
    }
}
