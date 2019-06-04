plugins {
    `java-library`
    jacoco
    id("io.franzbecker.gradle-lombok")
}

dependencies {
    //    compile("org.aspectj:aspectjweaver")
    api("javax.servlet:javax.servlet-api")
    api("org.springframework.boot:spring-boot-starter-actuator")
    api("org.springframework.boot:spring-boot-starter-data-jpa")
    api("org.springframework.boot:spring-boot-starter-data-rest")
    api(project(":f-tep-model"))
    api(project(":f-tep-search"))
    api(project(":f-tep-security"))

    implementation("com.fasterxml.jackson.datatype:jackson-datatype-hibernate5")
    implementation("com.querydsl:querydsl-jpa")
    implementation("com.squareup.okhttp3:logging-interceptor")
    implementation("com.squareup.okhttp3:okhttp")
    implementation("org.apache.commons:commons-text")
    implementation("org.apache.poi:poi")
    implementation("org.jooq:jool-java-8")
    implementation(project(":f-tep-batch"))
    implementation(project(":f-tep-catalogue"))
    implementation(project(":f-tep-costing"))
    implementation(project(":f-tep-defaultservices"))
    implementation(project(":f-tep-logging"))
    implementation(project(":f-tep-orchestrator"))
    implementation(project(":f-tep-persistence"))
    implementation(project(":f-tep-rpc"))
    
    runtimeOnly("org.springframework.data:spring-data-rest-hal-browser")

    testImplementation("org.hamcrest:hamcrest-junit")
    testImplementation("org.springframework.boot:spring-boot-starter-test")

    testRuntimeOnly("org.hsqldb:hsqldb")
}