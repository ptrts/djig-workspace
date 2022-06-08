package gitlabContainer

import org.apache.commons.io.FileUtils
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import java.io.FileReader
import java.io.IOException
import java.net.URI
import java.net.URISyntaxException
import java.util.*

class GitLabParameters(
    val gitlabUri: URI,
    val projectUri: URI,
    val username: String,
    val password: String
) {

    companion object {

        @Throws(IOException::class, URISyntaxException::class)
        fun getGitLabParameters(project: Project, propertiesResourcePath: String): GitLabParameters {

            val appProject = project.project(":app")

            val javaPluginExtension = appProject.extensions.findByType(
                JavaPluginExtension::class.java
            )!!

            val resourcesDir = javaPluginExtension
                .sourceSets
                .getByName("main")
                .resources
                .srcDirs
                .stream()
                .findFirst()
                .orElseThrow()

            val localGitlabPropertiesFile = FileUtils.getFile(resourcesDir, propertiesResourcePath)

            val localGitlabProperties = Properties()
            FileReader(localGitlabPropertiesFile).use { reader ->
                localGitlabProperties.load(reader)
            }

            val propertiesProjectUrlStr = localGitlabProperties.getProperty("dynamic-impl.git-repository.url")
            val projectUri = URI(propertiesProjectUrlStr)

            return GitLabParameters(
                username = localGitlabProperties.getProperty("dynamic-impl.git-repository.username"),
                password = localGitlabProperties.getProperty("dynamic-impl.git-repository.password"),
                projectUri = projectUri,
                gitlabUri = URI(
                    projectUri.scheme,
                    projectUri.authority,
                    null,
                    null,
                    null
                )
            )
        }
    }
}
