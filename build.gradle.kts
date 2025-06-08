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
    const val VERSION = "1.7.6"
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

        // 云标-云效制品库地址
        repositories {
            this.maven {
                url = URI("https://packages.aliyun.com/6825856bf9ff7623b1ecd70a/maven/starship-release")
                credentials {
                    username = "68258526c7c99a91a4d7f4ba"
                    password = "J)h7ptbn]2Gb"
                }
            }
        }
    }
}