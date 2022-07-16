package gitlabContainer.utils

import org.apache.commons.io.FileUtils
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import java.io.File
import java.io.FileReader
import java.net.URI
import java.net.URL
import java.util.*
import java.util.regex.Pattern

// todo Сие наверное надо тоже вынести в библиотеку или плагин Gradle, и отвязать от наших проектов "example-"
//      Может быть, этот класс нужно как-то объединить с теми пропертями, которые у нас используются в самом приложении

class DynamicProjectProperties(
    val gitlabUrl: URL,
    val projectUrl: URL,
    /**
     * Holds the username if the username+password authentication is used (oauth2).
     * In case of authentication with a personal/project/group access token or an impersonation token holds the token
     * (if so, [password] must be null).
     */
    val username: String,
    val password: String?
) {
    companion object {

        private val dynamicProjectPropertyNamePattern: Pattern = Pattern.compile(
            "^djig\\.dynamic-projects\\.(?<projectName>[^.]+)\\.(?<shortPropertyName>.+)$"
        )

        fun create(
            projectUrl: URL,
            username: String,
            password: String?
        ): DynamicProjectProperties {
            val projectUri: URI = projectUrl.toURI()
            return DynamicProjectProperties(
                username = username,
                password = password,
                projectUrl = projectUrl,
                gitlabUrl = URI(
                    projectUri.scheme,
                    projectUri.authority,
                    null,
                    null,
                    null
                ).toURL()
            )
        }

        fun loadDynamicProjectsMapFromAppProjectResource(
            project: Project,
            appProjectDirectoryRelativePath: String,
            propertiesResourcePath: String
        ): Map<String, DynamicProjectProperties> {
            val propertiesFile: File = FileUtils.getFile(
                project.projectDir,
                "${appProjectDirectoryRelativePath}/src/main/resources/${propertiesResourcePath}"
            )
            val properties: Properties = loadPropertiesFromFile(propertiesFile)
            val dynamicProjectsRawMap = loadDynamicProjectsMapRaw(properties)
            return getDynamicProjectsMapFromRawMap(dynamicProjectsRawMap)
        }

        private fun loadDynamicProjectsMapRaw(properties: Properties): MutableMap<String, MutableMap<String, String>> {
            val dynamicProjectsMapRaw = mutableMapOf<String, MutableMap<String, String>>()

            properties.forEach { propertyName, propertyValue ->
                val matcher = dynamicProjectPropertyNamePattern.matcher(propertyName as String)
                if (matcher.find()) {
                    val projectName = matcher.group("projectName")
                    val shortPropertyName = matcher.group("shortPropertyName")

                    val smallMap: MutableMap<String, String> = dynamicProjectsMapRaw.computeIfAbsent(projectName) {
                        return@computeIfAbsent mutableMapOf<String, String>()
                    }

                    smallMap[shortPropertyName] = propertyValue as String
                }
            }

            return dynamicProjectsMapRaw
        }

        private fun getDynamicProjectsMapFromRawMap(dynamicProjectsMapRaw: MutableMap<String, MutableMap<String, String>>): MutableMap<String, DynamicProjectProperties> {
            val dynamicProjectsMap = mutableMapOf<String, DynamicProjectProperties>()
            dynamicProjectsMapRaw.forEach { projectName, projectPropertiesMap ->
                dynamicProjectsMap[projectName] = parseProjectPropertiesMap(projectPropertiesMap)
            }
            return dynamicProjectsMap
        }

        private fun parseProjectPropertiesMap(projectPropertiesMap: MutableMap<String, String>): DynamicProjectProperties {
            val propertiesProjectUrlStr = projectPropertiesMap["url"]!!
            val projectUrl = URL(propertiesProjectUrlStr)

            val username = projectPropertiesMap["username"]!!
            val password = projectPropertiesMap["password"]!!
            return create(projectUrl, username, password)
        }

        private fun loadPropertiesFromFile(file: File): Properties {
            val localGitlabProperties = Properties()
            FileReader(file).use { reader ->
                localGitlabProperties.load(reader)
            }
            return localGitlabProperties
        }
    }
}
