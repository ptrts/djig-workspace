package djig

import gitlabContainer.utils.DynamicProjectProperties
import org.gitlab4j.api.GitLabApi
import org.gitlab4j.api.GitLabApiException
import org.gitlab4j.api.models.Group
import org.gitlab4j.api.models.Namespace
import org.gitlab4j.api.models.Project
import java.net.URL

object LocalGitLabProjectCreator {

    fun recreateGroupAndProject(
        dynamicProjectProperties: DynamicProjectProperties,
        targetGitLabUrl: URL,
        targetGitLabUsername: String,
        targetGitLabPassword: String?
    ): Project {

        val sourceGitLabApi: GitLabApi = getGitLabApi(
            dynamicProjectProperties.gitlabUrl,
            dynamicProjectProperties.username,
            dynamicProjectProperties.password
        )

        val sourceGitLabProject: Project = sourceGitLabApi.projectApi.getProject(dynamicProjectProperties.gitlabUrl.path)

        // todo Проверить выкачивание приватных проектов и их закачивание в локальный GitLab
        //      Может быть стоит все выкачиваемое делать публичным, даже если в оригинале оно приватное

        val targetGitLabApi: GitLabApi = getGitLabApi(
            targetGitLabUrl,
            targetGitLabUsername,
            targetGitLabPassword
        )

        val deepestTargetGroup = copyProjectGroups(sourceGitLabApi, sourceGitLabProject, targetGitLabApi)

        return recreateProject(targetGitLabApi, deepestTargetGroup, sourceGitLabProject.name)
    }

    private fun copyProjectGroups(sourceGitLabApi: GitLabApi, sourceGitLabProject: Project, targetGitLabApi: GitLabApi): Group? {
        var sourceGroups: List<Group> = getProjectGroups(sourceGitLabApi, sourceGitLabProject)

        var previousTargetGroup: Group? = null

        for (sourceGroup in sourceGroups) {
            previousTargetGroup = copyGroup(sourceGroup, targetGitLabApi, previousTargetGroup)
        }

        return previousTargetGroup
    }

    private fun getProjectGroups(gitLabApi: GitLabApi, project: Project): List<Group> {
        val groups: MutableList<Group> = ArrayList<Group>()

        val namespace: Namespace? = project.namespace
        if (namespace != null && "group".equals(namespace.kind)) {
            var group: Group? = gitLabApi.groupApi.getGroup(namespace.id)
            while (group != null) {
                groups.add(group)
                group = group.parentId?.let(gitLabApi.groupApi::getGroup)
            }
        }

        return groups.asReversed()
    }

    private fun copyGroup(
        sourceGroup: Group,
        targetGitLabApi: GitLabApi,
        targetParentGroup: Group?
    ): Group? {
        var targetGroup = Group()
            .withName(sourceGroup.name)
            .withPath(sourceGroup.path)
            .withDescription(sourceGroup.description)
            .withVisibility(sourceGroup.visibility)
            .withlfsEnabled(sourceGroup.lfsEnabled)
            .withRequestAccessEnabled(sourceGroup.requestAccessEnabled)
            .withParentId(targetParentGroup?.parentId)

        val pathPrefix =
            if (targetParentGroup == null)
                ""
            else
                (targetParentGroup.fullPath + "/")

        val targetGroupFullPath = pathPrefix + targetGroup.path

        return targetGitLabApi.groupApi.getOptionalGroup(targetGroupFullPath).orElseGet {
            targetGitLabApi.groupApi.addGroup(targetGroup)
        }
    }

    private fun getGitLabApi(url: URL, username: String, password: String?): GitLabApi {
        val urlStr = url.toString()
        if (password == null) {
            return GitLabApi(urlStr, username)
        } else {
            return GitLabApi.oauth2Login(urlStr, username, password, true)
        }
    }

    private fun recreateProject(gitLabApi: GitLabApi, group: Group?, projectName: String): Project {
        deleteProjectIfExistsAndWaitForDisappearance(gitLabApi, group, projectName)
        return createProject(gitLabApi, group, projectName)
    }

    private fun deleteProjectIfExistsAndWaitForDisappearance(gitLabApi: GitLabApi, group: Group?, projectName: String) {
        gitLabApi.projectApi
            .getOptionalProject(group?.path, projectName)
            .ifPresent { project ->
                gitLabApi.projectApi.deleteProject(project.id)
                waitForProjectDisappearance(gitLabApi, group, project)
            }
    }

    private fun waitForProjectDisappearance(gitLabApi: GitLabApi, group: Group?, project: Project) {
        println("Waiting until the existing project is deleted...")
        var i = 60
        while (i > 0) {
            if (gitLabApi.projectApi.getOptionalProject(group?.path, project.name).isPresent) {
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

    private fun createProject(gitLabApi: GitLabApi, group: Group?, projectName: String): Project {
        // There are attempts because after disappearing from the API a project may still exist for some time in GitLab underwhater,
        // making attempts to create another project with the same name and in the same namespace fail
        for (i in 100 downTo 1) {
            try {
                return gitLabApi.projectApi.createProject(group?.id, projectName)
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
