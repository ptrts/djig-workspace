package app

import gitlabContainer.GitLabParameters
import org.apache.commons.io.FileUtils
import org.apache.commons.lang3.StringUtils
import org.gitlab4j.api.GitLabApi
import org.gitlab4j.api.GitLabApiException
import org.gitlab4j.api.models.Group
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
        group = "app"

        description = """
        Creates a fork of dynamic-dev in the local GitLab
        and clones it into the dynamic-local project subdirectory
        """
    }

    @TaskAction
    fun action() {

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

        // We will create the dynamic-local project as a fork of dynamic-dev

        // Get the dynamic-dev Git repository URL from a profile property file of the main application "app"
        // App knows the URL because it's where it gets dynamic Java code from working in one of the profiles
        val devGitLabParameters: GitLabParameters =
            GitLabParameters.getGitLabParameters(project, "application-dynamic-dev.properties")

        // Cloning dynamic-dev into the dynamic-local subdirectory in the project
        GitUtils.clone(devGitLabParameters.projectUri.toString(), dynamicLocalDirectory)

        // Getting the dynamic-local Git repo properties from a profile property file of the main application "app"
        // Again, app knows the URL because it's where it gets dynamic Java code from working in one of the profiles
        val localGitLabParameters: GitLabParameters =
            GitLabParameters.getGitLabParameters(project, "application-dynamic-local.properties")

        // Configuring the new local Git repo
        // The user should by now have been created in the local GitLab by another Gradle task of ours
        ProcessRunner.runProcess(dynamicLocalDirectory, "git", "config", "--local", "user.name", localGitLabParameters.username)
        ProcessRunner.runProcess(
            dynamicLocalDirectory,
            "git",
            "config",
            "--local",
            "user.email",
            "${localGitLabParameters.username}@mail.com"
        )

        // Changing the name of the new fork project from dynamic-dev (which is being forked) to dynamic-local
        val settingsGradleFile: File = FileUtils.getFile(dynamicLocalDirectory, "settings.gradle.kts")
        FileUtils.writeStringToFile(settingsGradleFile, "rootProject.name = \"dynamic-local\"", StandardCharsets.UTF_8)
        ProcessRunner.runProcess(dynamicLocalDirectory, "git", "add", "*")
        ProcessRunner.runProcess(dynamicLocalDirectory, "git", "commit", "-m", "Changing project name to dynamic-local")

        // Create a GitLabApi instance to communicate with your GitLab server
        val gitLabApi: GitLabApi = GitLabApi.oauth2Login(
            localGitLabParameters.gitlabUri.toString(),
            localGitLabParameters.username,
            localGitLabParameters.password,
            true
        )

        // todo Break all this into parts. It's become hard to understand the variable names

        // Breaking the path of the dynamic-local Git repo URL into parts
        val pathParts: List<String> = localGitLabParameters.projectUri.path
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

        // Find or create the project group in the local GitLab
        val group: Group = gitLabApi.groupApi.getOptionalGroup(groupPath)
            .orElseGet {
                gitLabApi.groupApi.addGroup(groupPath, groupPath)
            }

        var gitlabProject: org.gitlab4j.api.models.Project? = null

        // Removing the project in the local GitLab if exists
        gitLabApi.projectApi.getOptionalProject(groupPath, projectName)
            .ifPresent {
                gitLabApi.projectApi.deleteProject(it.id)
                println("Waiting until the existing project is deleted...")
                var i = 60
                while (i > 0) {
                    gitlabProject = gitLabApi.projectApi.getOptionalProject(groupPath, projectName).orElse(null)
                    if (gitlabProject == null) {
                        break
                    }
                    println("$i...")
                    Thread.sleep(1000)
                    i--
                }
                if (i == 0) {
                    throw RuntimeException("Could not delete project $projectName")
                }
            }
        // Trying to create the project
        // There are attempts because an attempt may fail if the project is still in the process of being deleted
        for (i in 100 downTo 1) {
            try {
                gitlabProject = gitLabApi.projectApi.createProject(group.id, projectName)
                break
            } catch (e: GitLabApiException) {
                if (i == 1) {
                    throw e
                } else {
                    e.validationErrors.forEach { (key, value) ->
                        println("Gitlab project creation error: attempt $i, key = $key, value = $value")
                    }
                    Thread.sleep(1000)
                }
            }
        }

        assert(gitlabProject != null)

        // Adding credentials to the Git repo URI of the project in the local GitLab
        val remoteUriWithCredentials = URI(
            localGitLabParameters.projectUri.scheme,
            "${localGitLabParameters.username}:${localGitLabParameters.password}",
            localGitLabParameters.projectUri.host,
            localGitLabParameters.projectUri.port,
            localGitLabParameters.projectUri.path,
            null,
            null
        )

        // Setting the Git repo URL as remote for the local Git repo
        ProcessRunner.runProcess(dynamicLocalDirectory, "git", "remote", "set-url", "origin", remoteUriWithCredentials.toString())

        // Pushing the project into the local GitLab
        ProcessRunner.runProcess(dynamicLocalDirectory, "git", "push", "origin", "master")
    }
}
