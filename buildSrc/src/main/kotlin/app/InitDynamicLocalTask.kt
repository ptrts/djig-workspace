package app

import gitlabContainer.utils.GitLabParameters
import org.apache.commons.io.FileUtils
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.taruts.gitUtils.GitUtils
import org.taruts.processUtils.ProcessRunner
import java.io.File
import java.net.URI
import java.nio.charset.StandardCharsets
import java.nio.file.Path

open class InitDynamicLocalTask : DefaultTask() {

    init {
        group = "workspace"

        description = """
        Creates a fork of dynamic-dev in the dynamic-local project subdirectory 
        and pushes it to the local GitLab
        """.trimIndent()
    }

    private lateinit var dynamicLocalDirectory: File
    private lateinit var dynamicLocalGitLabParameters: GitLabParameters

    @TaskAction
    fun action() {
        dynamicLocalDirectory = getDynamicLocalSourceDir()

        // Getting the dynamic-local Git repo properties from a profile property file of the main application "app"
        // Again, app knows the URL because it's where it gets dynamic Java code from working in one of the profiles
        dynamicLocalGitLabParameters = GitLabParameters.fromAppProjectResource(
            project,
            "application-dynamic-local.properties"
        )

        cloneDynamicDev()
        configureLocalGitRepo()
        renameProject()
        pushToLocalGitLab()
    }

    private fun getDynamicLocalSourceDir(): File {
        // Determining the path of a directory to store the dynamic-local source code
        val dynamicLocalDirectory: File = Path
            .of(project.rootProject.projectDir.path, "dynamic-local")
            .toAbsolutePath()
            .normalize()
            .toFile()
        // Remove the source directory if it exists
        if (dynamicLocalDirectory.exists()) {
            FileUtils.forceDelete(dynamicLocalDirectory)
        }
        FileUtils.forceMkdir(dynamicLocalDirectory)
        return dynamicLocalDirectory
    }

    private fun cloneDynamicDev() {
        // Get the dynamic-dev Git repository URL from a profile property file of the main application "app"
        // App knows the URL because it's where it gets dynamic Java code from working in one of the profiles
        val devGitLabParameters = GitLabParameters.fromAppProjectResource(project, "application-dynamic-dev.properties")

        // Cloning dynamic-dev into the dynamic-local subdirectory in the project
        GitUtils.clone(devGitLabParameters.projectUri.toString(), dynamicLocalDirectory)
    }

    private fun configureLocalGitRepo() {
        // The user should by now have been created in the local GitLab by another Gradle task of ours
        ProcessRunner.runProcess(dynamicLocalDirectory, "git", "config", "--local", "user.name", dynamicLocalGitLabParameters.username)
        ProcessRunner.runProcess(
            dynamicLocalDirectory,
            "git", "config", "--local", "user.email", "${dynamicLocalGitLabParameters.username}@mail.com"
        )
    }

    private fun renameProject() {
        // Changing the name of the new fork project from "dynamic-dev" (which is being forked) to "dynamic-local"
        val settingsGradleFile: File = FileUtils.getFile(dynamicLocalDirectory, "settings.gradle.kts")
        FileUtils.writeStringToFile(settingsGradleFile, "rootProject.name = \"dynamic-local\"", StandardCharsets.UTF_8)
        ProcessRunner.runProcess(dynamicLocalDirectory, "git", "add", "*")
        ProcessRunner.runProcess(dynamicLocalDirectory, "git", "commit", "-m", "Changing project name to dynamic-local")
    }

    private fun pushToLocalGitLab() {
        LocalGitLabProjectCreator.recreateGroupAndProject(dynamicLocalGitLabParameters)

        // Setting the Git repo URL as remote for the local Git repo
        val remoteUriWithCredentials = addCredentialsToUri()
        ProcessRunner.runProcess(dynamicLocalDirectory, "git", "remote", "set-url", "origin", remoteUriWithCredentials.toString())

        // Pushing the project into the local GitLab
        ProcessRunner.runProcess(dynamicLocalDirectory, "git", "push", "origin", "master")
    }

    private fun addCredentialsToUri(): URI {
        val projectUri = dynamicLocalGitLabParameters.projectUri
        val username = dynamicLocalGitLabParameters.username
        val password = dynamicLocalGitLabParameters.password

        return URI(
            projectUri.scheme,
            "$username:$password",
            projectUri.host,
            projectUri.port,
            projectUri.path,
            null,
            null
        )
    }
}
