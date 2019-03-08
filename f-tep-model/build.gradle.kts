plugins {
    `java-library`
    jacoco
    id("io.franzbecker.gradle-lombok")
    id("com.ewerk.gradle.plugins.querydsl")
}

dependencies {
    // the `querydsl` configuration extends `compile`, so no fancy separation of api/implementation here
    compile("com.querydsl:querydsl-apt")
    compile("org.projectlombok:lombok")
    compile("javax.persistence:javax.persistence-api")
    compile("org.hibernate:hibernate-core")
    compile("com.fasterxml.jackson.core:jackson-core")
    compile("com.fasterxml.jackson.core:jackson-databind")
    compile("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml")
    compile("com.fasterxml.jackson.datatype:jackson-datatype-guava")
    compile("org.springframework.hateoas:spring-hateoas")
    compile("org.springframework.data:spring-data-rest-core")
    compile("org.springframework.security:spring-security-core")
    implementation(project(":f-tep-logging"))

    testImplementation("junit:junit")
}

sourceSets { getByName("main").java.srcDirs("${buildDir}/generated/source/querydsl/main") }