plugins {
    `java-library`
    jacoco
    id("io.freefair.lombok")
}

dependencies {
    api("com.squareup.okhttp3:okhttp")
    api("de.grundid.opendatalab:geojson-jackson")
    api("org.springframework.boot:spring-boot-autoconfigure")
    api("org.springframework:spring-core")
    api(project(":f-tep-model"))
    api(project(":f-tep-rpc"))
    
    implementation("com.jayway.jsonpath:json-path")
    implementation("com.squareup.okhttp3:logging-interceptor")
    implementation("it.geosolutions:geoserver-manager")
    implementation("org.apache.commons:commons-text")
    implementation("org.geotools:gt-epsg-extension")
    implementation("org.geotools:gt-epsg-hsql")
    implementation("org.geotools:gt-geojson")
    implementation("org.geotools:gt-geometry")
    implementation("org.geotools:gt-geotiff")
    implementation("org.geotools:gt-main")
    implementation("org.geotools:gt-referencing")
    implementation("org.geotools:gt-shapefile")
    implementation("org.geotools:gt-xml")
    implementation("org.springframework.cloud:spring-cloud-starter-netflix-eureka-client")
    implementation(project(":f-tep-logging"))
    implementation(project(":f-tep-persistence"))
    implementation(project(":f-tep-security"))

    testImplementation("com.google.jimfs:jimfs")
    testImplementation("com.squareup.okhttp3:mockwebserver")
    testImplementation("junit:junit")
    testImplementation("org.springframework:spring-test")
    testImplementation("org.springframework.boot:spring-boot-starter-test")

    testRuntimeOnly("org.hsqldb:hsqldb")
}
