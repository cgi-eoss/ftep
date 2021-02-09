plugins {
    `java-library`
    jacoco
    id("io.freefair.lombok")
}

dependencies {
    api(project(":f-tep-model"))
    api("org.springframework:spring-core")
    implementation("org.jooq:jool-java-8")
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml")
    implementation(project(":f-tep-logging"))

    testImplementation("junit:junit")
}
