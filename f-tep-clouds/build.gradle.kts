plugins {
    `java-library`
    jacoco
    id("io.franzbecker.gradle-lombok")
}

dependencies {
    implementation("com.google.guava:guava")
    implementation("com.hierynomus:sshj")
    implementation("org.awaitility:awaitility")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation(project(":f-tep-logging"))
    implementation(project(":jclouds", "shadow")) { isTransitive = false }

    runtimeOnly("org.hsqldb:hsqldb")

    testImplementation("junit:junit")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
