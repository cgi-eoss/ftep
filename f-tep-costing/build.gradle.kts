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
    testImplementation("org.springframework.boot:spring-boot-starter-test")

    testRuntimeOnly("org.hsqldb:hsqldb")
}
