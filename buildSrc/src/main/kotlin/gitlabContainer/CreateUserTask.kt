package gitlabContainer

import gitlabContainer.utils.DockerShellRunner
import gitlabContainer.utils.GitLabParameters
import gitlabContainer.utils.ContainerMountPoints
import org.gitlab4j.api.GitLabApi
import org.gitlab4j.api.models.User
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import java.util.regex.Pattern

open class CreateUserTask : DefaultTask() {

    init {
        group = "gitlab-container"
    }

    @TaskAction
    fun action() {
        val fileContents: String = DockerShellRunner.runLinuxShellCommandInDockerWithMounts(
            project,
            false,
            "cat ${ContainerMountPoints.CONFIG}/initial_root_password"
        )

        val lines: List<String> = fileContents.split("\\n")

        val password: String = lines
            .stream()
            .map { line ->
                val pattern: Pattern = Pattern.compile("Password:\\s?(.+)")
                return@map pattern.matcher(line)
            }
            .filter { matcher -> matcher.find() }
            .map { matcher -> matcher.group(1) }
            .findAny()
            .orElseThrow { RuntimeException("Password not found") }

        val gitLabParameters = GitLabParameters.fromAppProjectResource(project, "application-dynamic-local.properties")

        val gitLabApi = GitLabApi.oauth2Login(gitLabParameters.gitlabUri.toString(), "root", password, true)

        val user = User()
        user.username = gitLabParameters.username
        user.name = gitLabParameters.username
        user.email = "${gitLabParameters.username}@mail.com"
        user.isAdmin = true
        user.canCreateGroup = true
        user.projectsLimit = 0
        user.sharedRunnersMinutesLimit = 0
        user.skipConfirmation = true

        gitLabApi.userApi.createUser(user, gitLabParameters.password, false)
    }
}
