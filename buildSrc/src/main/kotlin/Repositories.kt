import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.kotlin.dsl.maven
import java.net.URI

object Repositories {

    private const val DOMAIN = "https://packages.aliyun.com"
    private const val ALIYUN = "https://maven.aliyun.com/repository/public"
    private const val URL_RELEASE = "$DOMAIN/maven/repository/2218345-release-TG2hsk/"
    private const val URL_SNAPSHOTS = "$DOMAIN/maven/repository/2218345-snapshot-1Qs4uI/"
    private const val USERNAME = "625e0df381699e5a37856249"
    private const val PASSWORD = "2Nyd7z_poSkV"

    fun setRepositories(repositoryHandler: RepositoryHandler) {
        repositoryHandler.mavenLocal()
        repositoryHandler.maven(ALIYUN)
        repositoryHandler.maven(URL_RELEASE) {
            credentials {
                username = USERNAME
                password = PASSWORD
            }
        }
        repositoryHandler.maven(URL_SNAPSHOTS) {
            credentials {
                username = USERNAME
                password = PASSWORD
            }
        }
    }

    fun publishRepository(repositoryHandler: RepositoryHandler, version: String) {
        repositoryHandler.maven {
            url = URI(if (version.endsWith("SNAPSHOT")) URL_SNAPSHOTS else URL_RELEASE)
            credentials {
                username = USERNAME
                password = PASSWORD
            }
        }
    }

}