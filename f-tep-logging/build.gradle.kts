plugins {
    `java-library`
    jacoco
    id("io.freefair.lombok")
}

dependencies {
    api("org.apache.logging.log4j:log4j-api")

    runtimeOnly("com.lmax:disruptor")
    runtimeOnly("org.apache.logging.log4j:log4j-slf4j-impl")
    runtimeOnly("org.apache.logging.log4j:log4j-core")
    runtimeOnly("org.apache.logging.log4j:log4j-jul")
    runtimeOnly("org.graylog2.log4j2:log4j2-gelf")
    runtimeOnly("org.slf4j:jul-to-slf4j")
}
