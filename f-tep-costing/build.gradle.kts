plugins {
    `java-library`
    jacoco
    id("io.freefair.lombok")
}

dependencies {
    api("org.springframework:spring-core")

    implementation(project(":f-tep-logging"))
    implementation(project(":f-tep-persistence"))
    implementation(project(":f-tep-batch"))

    testImplementation("junit:junit")
    testImplementation("org.springframework:spring-test")

    testRuntimeOnly("org.hsqldb:hsqldb")
}
