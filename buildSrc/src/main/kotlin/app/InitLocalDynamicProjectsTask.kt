package app

import djig.DjigPluginExtension
import gitlabContainer.utils.DynamicProjectProperties
import org.apache.commons.io.FileUtils
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.taruts.gitUtils.GitUtils
import org.taruts.processUtils.ProcessRunner
import java.io.File
import java.net.URI
import java.net.URL
import java.nio.file.Path
import javax.inject.Inject

open class InitLocalDynamicProjectsTask
@Inject
constructor(
    @Input val projectNamePrefix: String = DEFAULT_PROJECT_NAME_PREFIX,
    @Input val springBootProfile: String = DEFAULT_SPRING_BOOT_PROFILE
) : DefaultTask() {
    companion object {
        val DEFAULT_PROJECT_NAME_PREFIX: String = "dynamic-local-"
        val DEFAULT_SPRING_BOOT_PROFILE: String = "dynamic-dev"
    }

    init {
        group = "djig"

        description = """
        Forks dynamic projects from application-${springBootProfile}.properties. 
        Each fork goes in the project subdirectory ${projectNamePrefix}<original project name>.
        The forks are also pushed to the local GitLab.
        """.trimIndent()
    }

    @TaskAction
    fun action() {
        // Get the remote URLs from the profile property file of the project.
        // The project knows the URLs because it's where it gets dynamic Java code when working in springBootProfile
        val devDynamicProjectsMap = DynamicProjectProperties.loadDynamicProjectsMapFromAppProjectResource(
            project, "application-${springBootProfile}.properties"
        )

        val djigPluginExtension: DjigPluginExtension = project.extensions.getByType(
            DjigPluginExtension::class.java
        )

        val targetGitLabUrl = djigPluginExtension.gitLab.url.get()
        val targetGitLabUsername = djigPluginExtension.gitLab.username.get()
        val targetGitLabPassword = djigPluginExtension.gitLab.password.get()

        // todo Кроме этого, сюда стоит добавить еще вот что
        //          Префикс группы
        //          Замена группы
        //          Постфикс группы

        devDynamicProjectsMap.forEach { projectName, dynamicProjectProperties ->
            forkProject(projectName, dynamicProjectProperties, targetGitLabUsername, targetGitLabUrl, targetGitLabPassword)
        }
    }

    private fun forkProject(
        projectName: String,
        dynamicProjectProperties: DynamicProjectProperties,
        targetGitLabUsername: String,
        targetGitLabUrl: URL,
        targetGitLabPassword: String
    ) {
        // Cloning a subdirectory in the project
        val dynamicLocalSourceDir = getDynamicLocalSourceDir(projectName)
        GitUtils.clone(dynamicProjectProperties.projectUri.toString(), dynamicLocalSourceDir)

        configureLocalGitRepo(dynamicLocalSourceDir, targetGitLabUsername)

        LocalGitLabProjectCreator.recreateGroupAndProject(
            dynamicProjectProperties, targetGitLabUrl, targetGitLabUsername, targetGitLabPassword
        );

        pushToLocalGitLab(dynamicLocalSourceDir, targetGitLabUrl, targetGitLabUsername, targetGitLabPassword)
    }

    private fun getDynamicLocalSourceDir(projectName: String): File {
        // Determining the path of the local source directory
        val dynamicLocalDirectory: File = Path
            .of(project.rootProject.projectDir.path, projectNamePrefix + projectName)
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

    private fun configureLocalGitRepo(dynamicLocalSourceDir: File, username: String) {
        // The user should by now have been created in the local GitLab by another Gradle task of ours
        ProcessRunner.runProcess(dynamicLocalSourceDir, "git", "config", "--local", "user.name", username)
        ProcessRunner.runProcess(
            dynamicLocalSourceDir,
            "git", "config", "--local", "user.email", "${username}@mail.com"
        )
    }

    private fun pushToLocalGitLab(dynamicLocalSourceDir: File, url: URL, username: String, password: String) {

        // Setting the Git repo URL as remote for the local Git repo
        val remoteUrlWithCredentials = GitUtils.addCredentialsToGitRepositoryUrl(url.toString(), username, password)
        ProcessRunner.runProcess(dynamicLocalSourceDir, "git", "remote", "set-url", "origin", remoteUrlWithCredentials.toString())

        // Pushing the project into the local GitLab
        ProcessRunner.runProcess(dynamicLocalSourceDir, "git", "push", "origin", "master")
    }
}
