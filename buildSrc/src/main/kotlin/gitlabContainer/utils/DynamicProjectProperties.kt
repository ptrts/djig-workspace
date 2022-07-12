package gitlabContainer.utils

import org.apache.commons.io.FileUtils
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import java.io.File
import java.io.FileReader
import java.net.URI
import java.util.*
import java.util.regex.Pattern

// todo Сие наверное надо тоже вынести в библиотеку или плагин Gradle, и отвязать от наших проектов "example-"
//      Может быть, этот класс нужно как-то объединить с теми пропертями, которые у нас используются в самом приложении

class DynamicProjectProperties(
    val gitlabUri: URI,
    val projectUri: URI,
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
            "^app\\.dynamic-projects\\.(?<projectName>[^.]+)\\.(?<shortPropertyName>.+)$"
        )

        fun loadDynamicProjectsMapFromAppProjectResource(project: Project, propertiesResourcePath: String): Map<String, DynamicProjectProperties> {
            val appProject = project.project(":example-app")
            val properties: Properties = loadPropertiesFromProjectResource(appProject, propertiesResourcePath)
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
            val projectUri = URI(propertiesProjectUrlStr)

            val dynamicProjectProperties = DynamicProjectProperties(
                username = projectPropertiesMap["username"]!!,
                password = projectPropertiesMap["password"]!!,
                projectUri = projectUri,
                gitlabUri = URI(
                    projectUri.scheme,
                    projectUri.authority,
                    null,
                    null,
                    null
                )
            )
            return dynamicProjectProperties
        }

        private fun loadPropertiesFromProjectResource(project: Project, propertiesResourcePath: String): Properties {
            val resourcesDir = getResourcesDirectory(project)

            val localGitlabPropertiesFile = FileUtils.getFile(resourcesDir, propertiesResourcePath)

            val localGitlabProperties = Properties()
            FileReader(localGitlabPropertiesFile).use { reader ->
                localGitlabProperties.load(reader)
            }
            return localGitlabProperties
        }

        private fun getResourcesDirectory(project: Project): File? {
            val javaPluginExtension = project.extensions.findByType(
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
            return resourcesDir
        }
    }
}
