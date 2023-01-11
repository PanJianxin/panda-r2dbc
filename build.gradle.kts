plugins {
    id("java")
    id("maven-publish")
    id("java-library")
    id("io.freefair.lombok") version "6.6"
}

allprojects {

    group = Project.GROUP
    version = Project.VERSION

    repositories {
        Repositories.setRepositories(this)
    }

    configurations {
        all {
            resolutionStrategy {
                force(SpringLibrary.SPRING_DATA_R2DBC)
                force(SpringLibrary.SPRING_R2DBC)
            }
        }
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

//        implementation(platform(Bom.R2DBC))
        implementation(Library.R2DBC_SPI)
        implementation(Library.R2DBC_POOL)

        implementation(platform(Bom.REACTOR))
        implementation(BomLibrary.REACTOR)
        implementation(BomLibrary.REACTOR_NETTY)

        implementation(platform(Bom.NETTY))
        implementation(BomLibrary.NETTY_HANDLER)

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
