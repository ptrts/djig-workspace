package djig.tasks

import org.gitlab4j.api.GitLabApi
import org.gitlab4j.api.GitLabApiException
import org.gitlab4j.api.models.Group
import org.gitlab4j.api.models.Namespace
import org.gitlab4j.api.models.Project
import java.net.URL

object LocalGitLabProjectCreator {

    fun recreateGroupAndProject(
        sourceProjectProperties: DynamicProjectProperties,
        targetGitLabUrl: URL,
        targetGitLabUsername: String,
        targetGitLabPassword: String?
    ): Project {

        val sourceGitLabApi: GitLabApi = getGitLabApi(
            sourceProjectProperties.gitlabUrl,
            sourceProjectProperties.username,
            sourceProjectProperties.password
        )

        val sourceProjectPath = cloneUrlPathToProjectPath(sourceProjectProperties.projectUrl.path)
        val sourceGitLabProject: Project = sourceGitLabApi.projectApi.getProject(sourceProjectPath)

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

    private fun getGitLabApi(url: URL, username: String?, password: String?): GitLabApi {
        val urlStr = url.toString()
        if (password == null) {
            return GitLabApi(urlStr, username)
        } else {
            return GitLabApi.oauth2Login(urlStr, username, password, true)
        }
    }

    private fun cloneUrlPathToProjectPath(clonePath: String): String {
        // todo Пока здесь обрабатываются только HTTP URL-ы. Также здесь надо обработать SSH URL-ы
        // todo Кроме того, здесь у нас все ориентировано на GitLab, не факт, что оно также будет работать в GitHub
        return httpCloneUrlToProjectPath(clonePath)
    }

    private fun httpCloneUrlToProjectPath(httpClonePath: String): String {
        return httpClonePath.replace(
            """^/(.*)\.git$""".toRegex(),
            "$1"
        )
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
            .withParentId(targetParentGroup?.id)

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

    private fun recreateProject(gitLabApi: GitLabApi, group: Group?, projectName: String): Project {
        deleteProject(gitLabApi, group, projectName)
        return createProject(gitLabApi, group, projectName)
    }

    private fun deleteProject(gitLabApi: GitLabApi, group: Group?, projectName: String) {
        println("Deleting existing project...")

        // todo Вынести количество секунд ожидания куда-нибудь в настройки
        var i = 300
        while (i > 0) {
            val project: Project? = gitLabApi.projectApi.getOptionalProject(group?.fullPath, projectName).orElse(null)
            if (project == null) {
                break
            }
            println("$i...")
            gitLabApi.projectApi.deleteProject(project.id)
            Thread.sleep(5000)
            i--
        }
        if (i == 0) {
            throw RuntimeException("Could not delete project ${projectName}")
        }
    }

    private fun createProject(gitLabApi: GitLabApi, group: Group?, projectName: String): Project {
        // There are attempts because after disappearing from the API a project may still exist for some time in GitLab underwhater,
        // making attempts to create another project with the same name and in the same namespace fail
        // todo Вынести куда-нибудь в настройки
        val waitPeriodSeconds = 240
        for (i in waitPeriodSeconds downTo 1) {
            try {
                val project: Project? = gitLabApi.projectApi.getOptionalProject(group?.fullPath, projectName).orElse(null)
                if (project != null) {
                    throw RuntimeException("Gitlab project creation error: attempt $i, there is a project by this path already")
                }
                return gitLabApi.projectApi.createProject(group?.id, projectName)
            } catch (e: GitLabApiException) {
                // Most probably, a project with the same name and in the same namespace is still in the process of deletion
                if (i > 1) {
                    val validationErrors = e.validationErrors
                    val message = if (validationErrors == null) {
                        e.message
                    } else {
                        validationErrors.entries.stream()
                            .findFirst()
                            .map { (key, value) -> "key = $key, value = $value" }
                            .orElse("no message")
                    }
                    println("Gitlab project creation error: attempt $i, $message")
                    Thread.sleep(5000)
                } else {
                    throw RuntimeException("Run out of attempts", e)
                }
            }
        }
        throw IllegalStateException("Exectution isn't supposed to reach here")
    }
}
