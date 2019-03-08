plugins {
    `java-library`
    jacoco
    id("io.franzbecker.gradle-lombok")
}

dependencies {
    api("org.springframework:spring-core")

    implementation(project(":f-tep-logging"))
    implementation(project(":f-tep-persistence"))

    testImplementation("junit:junit")
    testImplementation("org.springframework:spring-test")

    testRuntimeOnly("org.hsqldb:hsqldb")
}
