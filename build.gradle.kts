plugins {
    id("java")
    id("maven-publish")
    id("java-library")
    id("io.freefair.lombok") version "6.4.3"
}

allprojects {

    group = Project.GROUP
    version = Project.VERSION

    repositories {
        Repositories.setRepositories(this)
    }

}


subprojects {

    apply(plugin = "idea")
    apply(plugin = "java")
    apply(plugin = "java-library")
    apply(plugin = "maven-publish")
    apply(plugin = "io.freefair.lombok")

    java {
        sourceCompatibility = JavaVersion.VERSION_17
        withSourcesJar()
    }

    dependencies {

        implementation(platform(Bom.R2DBC))
        implementation(BomLibrary.R2DBC_SPI)
        implementation(BomLibrary.R2DBC_POOL)

        implementation(platform(Bom.REACTOR))
        implementation(BomLibrary.REACTOR)
        implementation(BomLibrary.REACTOR_NETTY)

        implementation(platform(Bom.NETTY))
        implementation(BomLibrary.NETTY_HANDLER)

        // https://mvnrepository.com/artifact/jakarta.annotation/jakarta.annotation-api
//        implementation("jakarta.annotation:jakarta.annotation-api:2.1.1")


//        testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.2")
//        testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.2")
    }

    publishing {

        repositories {
            Repositories.publishRepository(this, version.toString())
        }

        publications {
            create<MavenPublication>("mavenJava") {
                from(components["java"])
                groupId = Project.GROUP
                artifactId = tasks.jar.get().archiveBaseName.get()
            }
        }

    }

    tasks.getByName<Test>("test") {
        useJUnitPlatform()
    }

}
