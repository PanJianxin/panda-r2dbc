plugins {
    idea
    java
    `maven-publish`
    `java-library`
    alias(libs.plugins.lombok)
}

object Project {
    const val GROUP = "com.jxpanda.r2dbc"
    const val VERSION = "1.4.1"
}

allprojects {
    group = Project.GROUP
    version = Project.VERSION
}

configure(subprojects.filter { !it.name.endsWith("bom") }) {
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
        implementation(rootProject.libs.spotbugs.annotations)
    }
}

configure(subprojects.filter { it.name.startsWith("panda-r2dbc") }) {
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