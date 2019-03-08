plugins {
    `java-library`
    jacoco
    id("io.franzbecker.gradle-lombok")
}

dependencies {
    api(project(":f-tep-model"))
    api(project(":f-tep-rpc"))
    implementation(project(":f-tep-catalogue"))
    implementation(project(":f-tep-costing"))
    implementation(project(":f-tep-search"))
    implementation(project(":f-tep-persistence"))
    implementation(project(":f-tep-queues"))
    implementation(project(":f-tep-security"))
    implementation(project(":f-tep-logging"))
    implementation("org.jooq:jool-java-8")
    implementation("org.springframework:spring-jms")
    implementation("org.apache.commons:commons-lang3")
    implementation("org.springframework.cloud:spring-cloud-commons")
    implementation("org.springframework.cloud:spring-cloud-starter-netflix-eureka-client")
    implementation("org.apache.activemq:activemq-broker")

    testImplementation("junit:junit")
    testImplementation("org.mockito:mockito-core")
    testImplementation("org.hamcrest:hamcrest-junit")
    testImplementation("org.springframework:spring-test")
}
