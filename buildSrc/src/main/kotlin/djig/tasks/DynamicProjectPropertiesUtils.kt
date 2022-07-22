package djig.tasks

import common.PropertiesUtils
import java.io.File
import java.util.*
import java.util.regex.Pattern

object DynamicProjectPropertiesUtils {

    private val dynamicProjectPropertyNamePattern: Pattern = Pattern.compile(
        "^djig\\.dynamic-projects\\.(?<projectName>[^.]+)\\.(?<shortPropertyName>.+)$"
    )

    fun formatProperty(projectName: String, shortName: String, value: Any?): String {
        val valueStr: String = value?.toString() ?: ""
        return "djig.dynamic-projects.$projectName.$shortName=$valueStr"
    }

    fun loadDynamicProjectsFromSpringBootProperties(propertiesFile: File): Map<String, DynamicProjectProperties> {
        val properties: Properties = PropertiesUtils.loadPropertiesFromFile(propertiesFile)
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
            dynamicProjectsMap[projectName] = DynamicProjectProperties.fromMap(projectPropertiesMap)
        }
        return dynamicProjectsMap
    }
}
