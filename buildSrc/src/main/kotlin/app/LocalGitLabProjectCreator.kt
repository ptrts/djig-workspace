package app

import gitlabContainer.utils.GitLabParameters
import org.apache.commons.lang3.StringUtils
import org.gitlab4j.api.GitLabApi
import org.gitlab4j.api.GitLabApiException
import org.gitlab4j.api.models.Group
import org.gitlab4j.api.models.Project
import java.net.URI

object LocalGitLabProjectCreator {

    fun recreateGroupAndProject(localGitLabParameters: GitLabParameters) {
        val gitLabApi: GitLabApi = getGitLabApi(localGitLabParameters)
        val projectUriParceResult = parceRemoteUri(localGitLabParameters.projectUri)
        val group: Group = getOrCreateGroup(gitLabApi, projectUriParceResult.groupPath)
        recreateProject(gitLabApi, group, projectUriParceResult.projectName)
    }

    private fun getGitLabApi(gitLabParameters: GitLabParameters) = GitLabApi.oauth2Login(
        gitLabParameters.gitlabUri.toString(),
        gitLabParameters.username,
        gitLabParameters.password,
        true
    )

    private fun parceRemoteUri(remoteUri: URI): RemoteUriParceResult {

        // Breaking the path (what goes after the first "/") of the dynamic-local Git repo URL into parts
        val pathParts: List<String> = remoteUri.path
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

        return RemoteUriParceResult(groupPath, projectName)
    }

    private fun getOrCreateGroup(gitLabApi: GitLabApi, groupPath: String): Group {
        return gitLabApi.groupApi.getOptionalGroup(groupPath).orElseGet {
            gitLabApi.groupApi.addGroup(groupPath, groupPath)
        }
    }

    private fun recreateProject(gitLabApi: GitLabApi, group: Group, projectName: String): Project {
        deleteProjectIfExistsAndWaitForDisappearance(gitLabApi, group, projectName)
        return createProject(gitLabApi, group, projectName)
    }

    private fun deleteProjectIfExistsAndWaitForDisappearance(gitLabApi: GitLabApi, group: Group, projectName: String) {
        gitLabApi.projectApi
            .getOptionalProject(group.path, projectName)
            .ifPresent { project ->
                gitLabApi.projectApi.deleteProject(project.id)
                waitForProjectDisappearance(gitLabApi, group, project)
            }
    }

    private fun waitForProjectDisappearance(gitLabApi: GitLabApi, group: Group, project: Project) {
        println("Waiting until the existing project is deleted...")
        var i = 60
        while (i > 0) {
            if (gitLabApi.projectApi.getOptionalProject(group.path, project.name).isPresent) {
                break
            }
            println("$i...")
            Thread.sleep(1000)
            i--
        }
        if (i == 0) {
            throw RuntimeException("Could not delete project ${project.name}")
        }
    }

    private fun createProject(gitLabApi: GitLabApi, group: Group, projectName: String): Project {
        // There are attempts because after disappearing from the API a project may still exist for some time in GitLab,
        // making attempts to create another project with the same name and in the same namespace fail
        for (i in 100 downTo 1) {
            try {
                return gitLabApi.projectApi.createProject(group.id, projectName)
            } catch (e: GitLabApiException) {
                // Most probably, a project with the same name and in the same namespace is still in the process of deletion
                if (i > 1) {
                    e.validationErrors.forEach { (key, value) ->
                        println("Gitlab project creation error: attempt $i, key = $key, value = $value")
                    }
                    Thread.sleep(1000)
                } else {
                    throw e
                }
            }
        }
        throw IllegalStateException("Exectution isn't supposed to reach here")
    }

    private class RemoteUriParceResult(val projectName: String, val groupPath: String)
}
