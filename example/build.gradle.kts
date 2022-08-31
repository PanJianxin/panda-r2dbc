plugins {
    id("java")
    id("org.springframework.boot") version "2.7.2"
    id("io.spring.dependency-management") version "1.0.11.RELEASE"
}

group = "com.jxpanda.r2dbc"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {

    implementation(project(ProjectDependency.PANDA_R2DBC_SPRING_BOOT_STARTER))
//    implementation("org.springframework.boot:spring-boot-starter-data-r2dbc")
    implementation(project(ProjectDependency.MYSQL_CONNECTOR))
    //    implementation("dev.miku:r2dbc-mysql:0.8.2.RELEASE")
    implementation("org.springframework.boot:spring-boot-starter-webflux")

    implementation(Library.PANDA_COMMONS)
    implementation("io.projectreactor.addons:reactor-extra")

    // https://mvnrepository.com/artifact/io.r2dbc/r2dbc-spi
//    implementation("io.r2dbc:r2dbc-spi:1.0.0.RELEASE")
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    developmentOnly("org.springframework.boot:spring-boot-devtools")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.projectreactor:reactor-test")
    
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.2")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}