plugins {
    id("java")
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
    alias(libs.plugins.lombok)
}

group = "com.jxpanda.r2dbc"
version = "1.0-SNAPSHOT"

dependencies {
    implementation(libs.spring.boot.starter)
    implementation(project(":panda-r2dbc-spring-boot:panda-r2dbc-spring-boot-starter"))
    implementation("org.springframework.boot:spring-boot-starter-webflux")

    runtimeOnly(libs.db.mysql.r2dbc)
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.0")
    annotationProcessor(libs.spring.configuration.processor)
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}