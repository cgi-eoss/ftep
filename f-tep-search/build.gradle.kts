plugins {
    `java-library`
    jacoco
    id("io.franzbecker.gradle-lombok")
}

dependencies {
    api("org.springframework:spring-core")
    implementation(project(":f-tep-catalogue"))
    implementation(project(":f-tep-persistence"))
    implementation(project(":f-tep-security"))
    implementation(project(":f-tep-logging"))
    implementation("commons-net:commons-net")
    implementation("org.apache.commons:commons-text")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    implementation("com.squareup.okhttp3:okhttp")
    implementation("com.squareup.okhttp3:logging-interceptor")

    testCompile("junit:junit")
}
