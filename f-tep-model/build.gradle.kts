plugins {
    `java-library`
    jacoco
    id("io.freefair.lombok")
}

dependencies {
    // QueryDSL class generation
    annotationProcessor(group = "com.querydsl", name = "querydsl-apt", classifier = "hibernate")
    annotationProcessor("javax.persistence:javax.persistence-api")
    annotationProcessor("org.hibernate:hibernate-core")

    api("com.google.guava:guava")
    api("com.querydsl:querydsl-apt")
    api("javax.persistence:javax.persistence-api")
    api("org.hibernate:hibernate-core")
    api("org.springframework.hateoas:spring-hateoas")
    api("org.springframework.security:spring-security-core")
    api("com.fasterxml.jackson.core:jackson-core")
    api("com.fasterxml.jackson.core:jackson-databind")
    api("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml")
    api("com.fasterxml.jackson.datatype:jackson-datatype-guava")
    api("org.springframework.data:spring-data-rest-core")
    implementation(project(":f-tep-logging"))

    testImplementation("junit:junit")
}

sourceSets { getByName("main").java.srcDirs("${buildDir}/generated/source/querydsl/main") }