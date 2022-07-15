package djig

import gitlabContainer.utils.DynamicProjectProperties
import org.apache.commons.io.FileUtils
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.taruts.gitUtils.GitUtils
import org.taruts.processUtils.ProcessRunner
import java.io.File
import java.net.URL
import java.nio.file.Path
import javax.inject.Inject

open class InitLocalDynamicProjectsTask
@Inject
constructor(
    @Input val appProjectDirectoryRelativePath: String,
    @Input val sourceSpringBootProfile: String,
    @Input val targetGitLab: DjigPluginExtension.LocalGitLab
) : DefaultTask() {

    init {
        group = "djig"

        description = """
        Forks dynamic projects from application-${sourceSpringBootProfile}.properties. 
        Each fork goes in the project subdirectory ${targetGitLab.directoryPrefix.get()}<original project name>.
        The forks are also pushed to the local GitLab ${targetGitLab.name.get()}.
        """.trimIndent()
    }

    @TaskAction
    fun action() {
        // Get the remote URLs from the profile property file of the project.
        // The project knows the URLs because it's where it gets dynamic Java code when working in springBootProfile
        val sourceDynamicProjectsMap = DynamicProjectProperties.loadDynamicProjectsMapFromAppProjectResource(
            project, appProjectDirectoryRelativePath, "application-${sourceSpringBootProfile}.properties"
        )

        sourceDynamicProjectsMap.forEach { projectName, dynamicProjectProperties ->
            forkProject(projectName, dynamicProjectProperties, targetGitLab)
        }
    }

    private fun forkProject(
        projectName: String,
        dynamicProjectProperties: DynamicProjectProperties,
        targetGitLab: DjigPluginExtension.LocalGitLab
    ) {
        val targetGitLabUrl = targetGitLab.url.get()
        val targetGitLabUsername = targetGitLab.username.get()
        val targetGitLabPassword = targetGitLab.password.get()

        // Cloning a subdirectory in the project
        val dynamicLocalSourceDir = getDynamicLocalSourceDir(projectName)
        GitUtils.clone(dynamicProjectProperties.projectUrl.toString(), dynamicLocalSourceDir)

        configureLocalGitRepo(dynamicLocalSourceDir, targetGitLabUsername)

        LocalGitLabProjectCreator.recreateGroupAndProject(
            dynamicProjectProperties, targetGitLabUrl, targetGitLabUsername, targetGitLabPassword
        );

        pushToLocalGitLab(dynamicLocalSourceDir, targetGitLabUrl, targetGitLabUsername, targetGitLabPassword)
    }

    private fun getDynamicLocalSourceDir(projectName: String): File {
        // Determining the path of the local source directory
        val dynamicLocalDirectory: File = Path
            .of(project.rootProject.projectDir.path, targetGitLab.directoryPrefix.get() + projectName)
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

    private fun pushToLocalGitLab(dynamicLocalSourceDir: File, url: URL, username: String, password: String?) {

        // Setting the Git repo URL as remote for the local Git repo
        val remoteUrlWithCredentials = GitUtils.addCredentialsToGitRepositoryUrl(url.toString(), username, password)
        ProcessRunner.runProcess(dynamicLocalSourceDir, "git", "remote", "set-url", "origin", remoteUrlWithCredentials.toString())

        // Pushing the project into the local GitLab
        ProcessRunner.runProcess(dynamicLocalSourceDir, "git", "push", "origin", "master")
    }
}
