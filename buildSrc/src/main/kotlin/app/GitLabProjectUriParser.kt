package app

import org.apache.commons.lang3.StringUtils
import java.net.URI

object GitLabProjectUriParser {

    fun parse(projectUri: URI): Result {

        // Breaking the path (what goes after the first "/") of the dynamic-local Git repo URL into parts
        val pathParts: List<String> = projectUri.path
            .split("/")
            .stream()
            .filter(StringUtils::isNotBlank)
            .toList()

        // The last part is the .git "file" name
        val projectFileName: String = pathParts[pathParts.size - 1]
        // Removing the ".git" part to get the project name
        val dotIndex: Int = projectFileName.lastIndexOf(".")
        val projectName: String =
            if (dotIndex == -1)
                projectFileName
            else
                projectFileName.substring(0, dotIndex)

        // Getting the path of the group of the project
        // It's everything in the project path before the project .git "file"
        val groupPath: String = pathParts.subList(0, pathParts.size - 1).joinToString("/")

        return Result(groupPath, projectName)
    }

    class Result(val projectName: String, val groupPath: String)
}
