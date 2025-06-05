import java.net.URI

plugins {
    idea
    java
    `maven-publish`
    `java-library`
    alias(libs.plugins.lombok)
}

object Project {
    const val GROUP = "com.jxpanda.r2dbc"
    const val VERSION = "1.7.3"
}

allprojects {
    apply(plugin = "java")
    apply(plugin = "java-library")
    apply(plugin = rootProject.libs.plugins.lombok.get().pluginId)


    group = Project.GROUP
    version = Project.VERSION

    java {
        sourceCompatibility = JavaVersion.VERSION_17
        withSourcesJar()
    }

}


configure(subprojects.filter { it.name.startsWith("panda-r2dbc") }) {
    apply(plugin = "idea")
    apply(plugin = "maven-publish")

    dependencies{
        implementation(rootProject.libs.spring.boot.starter)
    }

    publishing {
        publications {
            create<MavenPublication>("mavenJava") {
                from(components["java"])
                groupId = Project.GROUP
                artifactId = tasks.jar.get().archiveBaseName.get()
            }
        }
    }
}