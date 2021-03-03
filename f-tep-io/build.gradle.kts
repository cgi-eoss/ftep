plugins {
    `java-library`
    jacoco
    id("io.freefair.lombok")
}

dependencies {
    implementation(project(":f-tep-logging"))
    implementation(project(":f-tep-rpc"))

    implementation("com.jayway.jsonpath:json-path")
    implementation("com.squareup.okhttp3:logging-interceptor")
    implementation("com.squareup.okhttp3:okhttp")
    implementation("org.jooq:jool-java-8")
    implementation("commons-io:commons-io")
    implementation("commons-net:commons-net")
    implementation("org.apache.commons:commons-text")
    implementation("org.springframework.boot:spring-boot")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework.cloud:spring-cloud-starter-netflix-eureka-client")
    implementation("org.springframework.security:spring-security-oauth2-client")
    implementation("net.jodah:failsafe")

    testImplementation(project(":f-tep-persistence"))
    testImplementation("org.hamcrest:hamcrest-junit")
    testImplementation("org.mockito:mockito-core")
    testImplementation("com.google.jimfs:jimfs")
    testImplementation("com.squareup.okhttp3:mockwebserver")
    testImplementation("junit:junit")
    testImplementation("org.mockftpserver:MockFtpServer")
}
