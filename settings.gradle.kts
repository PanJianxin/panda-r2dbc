@file:Suppress("UnstableApiUsage")

rootProject.name = "panda-r2dbc"
include("example")
include("panda-r2dbc-bom")
include("panda-r2dbc-core")
include("panda-r2dbc-extension")
include("panda-r2dbc-spring-boot")
include("panda-r2dbc-spring-boot:panda-r2dbc-spring-boot-autoconfigure")
include("panda-r2dbc-spring-boot:panda-r2dbc-spring-boot-starter")

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("./settings-libs.toml"))
        }
    }
}

dependencyResolutionManagement {
    val username = "625e0df381699e5a37856249"
    val password = "2Nyd7z_poSkV"
    repositories {
        mavenLocal()
        mavenCentral()
        maven("https://maven.aliyun.com/repository/public")
        maven("https://packages.aliyun.com/maven/repository/2218345-release-TG2hsk/") {
            credentials {
                this.username = username
                this.password = password
            }
        }
        maven("https://packages.aliyun.com/maven/repository/2218345-snapshot-1Qs4uI/") {
            credentials {
                this.username = username
                this.password = password
            }
        }
    }
}