plugins {
    `java-library`
    jacoco
    id("io.freefair.lombok")
}

dependencies {
    implementation(project(":f-tep-logging"))
    implementation(project(":f-tep-model"))
    implementation(project(":f-tep-persistence"))
    implementation("com.google.guava:guava")
    implementation("javax.servlet:javax.servlet-api")
    implementation("javax.cache:cache-api")
    implementation("org.apache.commons:commons-lang3")

    api("org.springframework.boot:spring-boot-autoconfigure")
    api("org.springframework.security:spring-security-acl")
    api("org.springframework.security:spring-security-web")
    api("org.springframework.security:spring-security-config")
}
