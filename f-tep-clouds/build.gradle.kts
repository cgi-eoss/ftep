plugins {
    `java-library`
    jacoco
    id("io.franzbecker.gradle-lombok")
}

dependencies {
    implementation("com.google.guava:guava")
    implementation("com.hierynomus:sshj")
    implementation("org.apache.jclouds.api:openstack-cinder")
    implementation("org.apache.jclouds.api:openstack-keystone")
    implementation("org.apache.jclouds.api:openstack-neutron")
    implementation("org.apache.jclouds.api:openstack-nova")
    implementation("org.apache.jclouds.driver:jclouds-okhttp")
    implementation("org.apache.jclouds.driver:jclouds-slf4j")
    implementation("org.apache.jclouds.driver:jclouds-sshj")
    implementation("org.awaitility:awaitility")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation(project(":f-tep-logging"))

    runtimeOnly("org.hsqldb:hsqldb")

    testImplementation("junit:junit")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
