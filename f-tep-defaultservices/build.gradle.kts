plugins {
    `java-library`
    jacoco
    id("io.franzbecker.gradle-lombok")
}

dependencies {
    api(project(":f-tep-model"))
    api("org.springframework:spring-core")
    implementation("org.jooq:jool-java-8")
    implementation(project(":f-tep-logging"))

    testImplementation("junit:junit")
}
