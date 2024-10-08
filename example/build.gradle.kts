plugins {
    id("java")
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
}

group = "com.jxpanda.r2dbc"
version = "1.0-SNAPSHOT"

dependencies {
    implementation(project(":panda-r2dbc-spring-boot:panda-r2dbc-spring-boot-starter"))
    implementation("org.springframework.boot:spring-boot-starter-webflux")
//    implementation("com.jxpanda.commons:commons-base:1.2.9")

    implementation(libs.r2dbc.postgresql)

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.0")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}