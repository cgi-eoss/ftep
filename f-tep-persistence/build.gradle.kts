plugins {
    `java-library`
    jacoco
    id("io.franzbecker.gradle-lombok")
}

dependencies {
    implementation(project(":f-tep-logging"))
    implementation("com.google.guava:guava")
    implementation("com.querydsl:querydsl-jpa")
    implementation("org.springframework.data:spring-data-jpa")

    api(project(":f-tep-model"))
    api(project(":f-tep-rpc"))

    runtimeOnly("org.flywaydb:flyway-core")

    testImplementation("junit:junit")
    testImplementation("org.springframework:spring-test")
    testImplementation("org.springframework.boot:spring-boot-test")
    testImplementation("org.springframework.boot:spring-boot-test-autoconfigure")
    testImplementation("io.zonky.test:embedded-database-spring-test")
    testImplementation("org.hamcrest:hamcrest-junit")

    testRuntimeOnly("org.hsqldb:hsqldb")
    testRuntimeOnly("org.postgresql:postgresql")
}
