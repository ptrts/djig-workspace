package gitlabContainer

import gitlabContainer.utils.DockerShellRunner
import gitlabContainer.utils.GitLabContainerMountPoints
import org.gitlab4j.api.GitLabApi
import org.gitlab4j.api.models.User
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import java.net.URL
import java.util.regex.Pattern

open class CreateUserTask : DefaultTask() {

    init {
        group = "gitlab-container"
    }

    @TaskAction
    fun action() {

        val gitLabContainerPluginExtension = project.extensions.findByType(
            GitLabContainerPluginExtension::class.java
        )!!

        val url: URL = gitLabContainerPluginExtension.url.get()
        val username: String = gitLabContainerPluginExtension.username.get()
        val password: String = gitLabContainerPluginExtension.password.get()

        val gitLabApi = loginToGitLabApiAsRoot(url)

        val user = User()
        user.username = username
        user.name = username
        user.email = "${username}@mail.com"
        user.isAdmin = true
        user.canCreateGroup = true
        user.projectsLimit = 0
        user.sharedRunnersMinutesLimit = 0
        user.skipConfirmation = true

        gitLabApi.userApi.createUser(user, password, false)
    }

    private fun loginToGitLabApiAsRoot(url: URL): GitLabApi {
        val initialRootPassword: String = getInitialRootPassword()

        val gitLabApi = GitLabApi.oauth2Login(
            url.toString(),
            "root",
            initialRootPassword,
            true
        )

        return gitLabApi
    }

    private fun getInitialRootPassword(): String {
        val fileContents: String = DockerShellRunner.runCommandInDockerWithGitLabMounts(
            project,
            false,
            "cat ${GitLabContainerMountPoints.CONFIG}/initial_root_password"
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

        return password
    }
}
