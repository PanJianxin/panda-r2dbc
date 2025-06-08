@file:Suppress("UnstableApiUsage")

rootProject.name = "panda-r2dbc"
include("example")
include("panda-r2dbc-core")
include("panda-r2dbc-extension")
include("panda-r2dbc-spring-boot:panda-r2dbc-spring-boot-autoconfigure")
include("panda-r2dbc-spring-boot:panda-r2dbc-spring-boot-starter")

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from("com.jxpanda:version-catalog:1.0.20")
        }
    }
}

dependencyResolutionManagement {
    val username = "68258526c7c99a91a4d7f4ba"
    val password = "J)h7ptbn]2Gb"
    repositories {
        mavenLocal()
        mavenCentral()
        maven("https://packages.aliyun.com/6825856bf9ff7623b1ecd70a/maven/starship-release") {
            credentials {
                this.username = username
                this.password = password
            }
        }
    }
}