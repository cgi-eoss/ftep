plugins {
    `java-library`
    jacoco
    id("io.franzbecker.gradle-lombok")
}

dependencies {
    implementation(project(":f-tep-logging"))
    implementation("org.springframework:spring-jms")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.apache.activemq:activemq-broker")
    implementation("org.apache.activemq:activemq-pool")

    testImplementation("junit:junit")
    testImplementation("org.springframework:spring-test")
}
