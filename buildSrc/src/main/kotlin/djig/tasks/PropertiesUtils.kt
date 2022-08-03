package djig.tasks

import java.io.File
import java.io.FileReader
import java.util.*

object PropertiesUtils {
    fun loadPropertiesFromFile(file: File): Properties {
        val localGitlabProperties = Properties()
        FileReader(file).use { reader ->
            localGitlabProperties.load(reader)
        }
        return localGitlabProperties
    }
}