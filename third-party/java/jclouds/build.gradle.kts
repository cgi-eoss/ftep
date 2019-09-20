plugins {
    `java-library`
    id("com.github.johnrengelman.shadow")
}

dependencies {
    api("org.apache.jclouds.api:openstack-cinder")
    api("org.apache.jclouds.api:openstack-keystone")
    api("org.apache.jclouds.api:openstack-neutron")
    api("org.apache.jclouds.api:openstack-nova")
    api("org.apache.jclouds.driver:jclouds-okhttp")
    api("org.apache.jclouds.driver:jclouds-slf4j")
    api("org.apache.jclouds.driver:jclouds-sshj")

    // force the specific versions of jclouds dependencies
    implementation("com.google.inject:guice:3.0")
    implementation("com.google.code.gson:gson:2.5")
    implementation("com.google.errorprone:error_prone_annotations:2.1.0")
    implementation("com.google.guava:guava:18.0")
    implementation("com.squareup.okhttp:okhttp:2.2.0")
    implementation("javax.ws.rs:javax.ws.rs-api:2.0.1")
}

configurations.compile {
    exclude(group = "org.slf4j")
}

// Provide a Serializable config property to fix shadowJar caching
data class RelocationConfiguration(
    val pattern: String,
    val destination: String,
    val exclusionPatterns: List<String> = listOf()
) : java.io.Serializable

val relocationConfigs = listOf(
    "com.google.code.gson",
    "com.google.common",
    "com.google.gson",
    "com.google.inject",
    "com.google.thirdparty",
    "okhttp",
    "okio",
    "javax.ws.rs"
).map {
    RelocationConfiguration(it, "shadow.jclouds.$it")
}

tasks {
    "shadowJar"(com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar::class) {
        relocationConfigs.forEach { relocationConfig ->
            relocate(relocationConfig.pattern, relocationConfig.destination) {
                relocationConfig.exclusionPatterns.forEach {
                    exclude(it)
                }
            }
        }
        inputs.property("relocationConfigs", relocationConfigs)
        mergeServiceFiles()
    }
}
