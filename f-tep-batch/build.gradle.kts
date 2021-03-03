plugins {
    `java-library`
    jacoco
    id("io.freefair.lombok")
}

dependencies {
    implementation(project(":f-tep-catalogue"))
    implementation(project(":f-tep-search"))
    implementation(project(":f-tep-persistence"))
    implementation(project(":f-tep-logging"))

    testImplementation("junit:junit")
    testImplementation("org.mockito:mockito-core")
    testImplementation("org.hamcrest:hamcrest-junit")
    testImplementation("org.springframework:spring-test")

    testRuntimeOnly("org.hsqldb:hsqldb")
}
