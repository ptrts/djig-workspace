package app

import gitlabContainer.utils.GitLabParameters
import org.gitlab4j.api.GitLabApi
import org.gitlab4j.api.GitLabApiException
import org.gitlab4j.api.models.Group
import org.gitlab4j.api.models.Project

object LocalGitLabProjectCreator {

    fun recreateGroupAndProject(localGitLabParameters: GitLabParameters) {
        val gitLabApi: GitLabApi = getGitLabApi(localGitLabParameters)
        val projectUriParceResult = GitLabProjectUriParser.parse(localGitLabParameters.projectUri)
        val group: Group = getOrCreateGroup(gitLabApi, projectUriParceResult.groupPath)
        recreateProject(gitLabApi, group, projectUriParceResult.projectName)
    }

    private fun getGitLabApi(gitLabParameters: GitLabParameters) = GitLabApi.oauth2Login(
        gitLabParameters.gitlabUri.toString(),
        gitLabParameters.username,
        gitLabParameters.password,
        true
    )

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
        // There are attempts because after disappearing from the API a project may still exist for some time in GitLab underwhater,
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
}
