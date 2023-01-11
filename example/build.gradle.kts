plugins {
    id("java")
    id("org.springframework.boot") version "3.0.1"
    id("io.spring.dependency-management") version "1.1.0"
}

group = "com.jxpanda.r2dbc"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {

    implementation(project(ProjectDependency.PANDA_R2DBC_SPRING_BOOT_STARTER))
//    implementation(project(ProjectDependency.PANDA_R2DBC_SPRING_DATA))
    implementation("org.springframework.boot:spring-boot-starter-data-r2dbc")
    implementation(project(ProjectDependency.MYSQL_CONNECTOR))
    //    implementation("dev.miku:r2dbc-mysql:0.8.2.RELEASE")
    // https://mvnrepository.com/artifact/com.github.jasync-sql/jasync-r2dbc-mysql
//    implementation("com.github.jasync-sql:jasync-r2dbc-mysql:2.1.7")

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

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.0")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}