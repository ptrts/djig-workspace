package djig.tasks

import org.taruts.propertyFileSectionUtils.PropertiesFileSectionUtils
import djig.DjigPluginExtension
import org.apache.commons.io.FileUtils
import org.gitlab4j.api.models.Project
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.taruts.gitUtils.GitUtils
import org.taruts.processUtils.ProcessRunner
import java.io.File
import java.net.URI
import java.net.URL
import java.nio.file.Path
import java.util.stream.Stream
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
        The forks are also pushed to the local GitLab ${targetGitLab.name}.
        """.trimIndent()
    }

    @TaskAction
    fun action() {
        val targetDynamicProjectsMap = forkProjects()
        writeTargetProfileDynamicProjectProperties(targetDynamicProjectsMap)
    }

    private fun forkProjects(): HashMap<String, DynamicProjectProperties> {
        // Get the remote URLs from the profile property file of the project.
        // The project knows the URLs because it's where it gets dynamic Java code when working in springBootProfile
        val sourceSpringBootProfilePropertiesFile: File = getProfilePropertiesFile(sourceSpringBootProfile)
        val sourceDynamicProjectsMap =
            DynamicProjectPropertiesUtils.loadDynamicProjectsFromSpringBootProperties(sourceSpringBootProfilePropertiesFile)

        val targetDynamicProjectsMap = HashMap<String, DynamicProjectProperties>()
        sourceDynamicProjectsMap.entries.forEach { (projectName, dynamicProjectProperties) ->
            val targetProjectProperties: DynamicProjectProperties = forkProject(projectName, dynamicProjectProperties, targetGitLab)
            targetDynamicProjectsMap[projectName] = targetProjectProperties
        }
        return targetDynamicProjectsMap
    }

    private fun forkProject(
        projectName: String,
        sourceProjectProperties: DynamicProjectProperties,
        targetGitLab: DjigPluginExtension.LocalGitLab
    ): DynamicProjectProperties {
        val targetGitLabUrl = targetGitLab.url.get()
        val targetGitLabUsername = targetGitLab.username.get()
        val targetGitLabPassword = targetGitLab.password.get()

        // Cloning a subdirectory in the project
        val dynamicLocalSourceDir = getDynamicLocalSourceDir(projectName)
        GitUtils.clone(sourceProjectProperties.projectUrl.toString(), dynamicLocalSourceDir)

        configureLocalGitRepo(dynamicLocalSourceDir, targetGitLabUsername)

        val targetProject: Project = LocalGitLabProjectCreator.recreateGroupAndProject(
            sourceProjectProperties, targetGitLabUrl, targetGitLabUsername, targetGitLabPassword
        )

        val projectUrl = replaceDomainNamePlaceholder(targetProject.httpUrlToRepo, targetGitLabUrl)

        val targetProjectProperties = DynamicProjectProperties.create(
            projectUrl = projectUrl,
            username = targetGitLabUsername,
            password = targetGitLabPassword,
            dynamicInterfacePackage = sourceProjectProperties.dynamicInterfacePackage
        )

        pushToLocalGitLab(dynamicLocalSourceDir, targetProjectProperties)

        return targetProjectProperties
    }

    private fun getDynamicLocalSourceDir(projectName: String): File {
        // Determining the path of the local source directory
        // todo Это "projects" - это должен быть либо параметр плагина workspace, либо константа этого плагина
        val dynamicLocalDirectory: File = Path
            .of(project.rootProject.projectDir.path, "projects", targetGitLab.directoryPrefix.get() + projectName)
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

    private fun replaceDomainNamePlaceholder(targetProjectUrlStr: String, targetGitLabUrl: URL): URL {
        val targetGitLabUri = targetGitLabUrl.toURI()
        var targetProjectUri = URI(targetProjectUrlStr)

        val domainNamePlaceholder = "gitlab.domain.name.placeholder"
        if (targetProjectUri.authority.equals(domainNamePlaceholder).not()) {
            throw IllegalStateException("The authority replaced is expected to be [$domainNamePlaceholder]")
        }

        targetProjectUri = URI(
            targetProjectUri.scheme,
            targetGitLabUri.authority,
            targetProjectUri.path,
            targetProjectUri.query,
            targetProjectUri.fragment
        )

        return targetProjectUri.toURL()
    }

    private fun pushToLocalGitLab(
        dynamicLocalSourceDir: File,
        targetProjectProperties: DynamicProjectProperties
    ) {
        val projectUrl = targetProjectProperties.projectUrl
        val username = targetProjectProperties.username
        val password = targetProjectProperties.password

        // Setting the Git repo URL as remote for the local Git repo
        val remoteUrlWithCredentials = GitUtils.addCredentialsToGitRepositoryUrl(projectUrl.toString(), username, password)
        ProcessRunner.runProcess(dynamicLocalSourceDir, "git", "remote", "set-url", "origin", remoteUrlWithCredentials.toString())

        // Pushing the project into the local GitLab
        ProcessRunner.runProcess(dynamicLocalSourceDir, "git", "push", "origin", "master")
    }

    private fun writeTargetProfileDynamicProjectProperties(targetDynamicProjectsMap: HashMap<String, DynamicProjectProperties>) {
        val targetSpringBootProfile = targetGitLab.springBootProfile.get()
        val targetSpringBootProfilePropertiesFile: File = getProfilePropertiesFile(targetSpringBootProfile)

        // Формируем объединенный стрим пропертей всех динамических проектов
        val allTaretProjectsPropertiesStream: Stream<String> =
            targetDynamicProjectsMap.entries.stream().flatMap { (projectName, dynamicProjectProperties) ->
                return@flatMap getDynamicProjectPropertiesStream(projectName, dynamicProjectProperties)
            }

        PropertiesFileSectionUtils.replaceSectionContents(
            file = targetSpringBootProfilePropertiesFile,
            // todo djig.dynamic-projects вынести в какую-нибудь константу. Может быть в утилах DynamicProjectPropertiesUtils
            sectionName = "djig.dynamic-projects",
            newContentsStream = allTaretProjectsPropertiesStream
        )
    }

    private fun getProfilePropertiesFile(springBootProfile: String): File {
        return StandaloneChildProjectUtils.getResourceFile(
            project,
            appProjectDirectoryRelativePath,
            "application-$springBootProfile.properties"
        )
    }

    private fun getDynamicProjectPropertiesStream(
        projectName: String,
        dynamicProjectProperties: DynamicProjectProperties
    ): Stream<String>? {
        val projectShortNamePropertiesMap: Map<String, String> = dynamicProjectProperties.toShortNameMap()

        val projectFullPropertyDefinitionsStream: Stream<String> =
            projectShortNamePropertiesMap.entries.stream().map { (shortPropertyName, propertyValueStr) ->
                return@map DynamicProjectPropertiesUtils.formatProperty(projectName, shortPropertyName, propertyValueStr)
            }

        // Adding an empty line after property definitions of each project
        return Stream.concat(projectFullPropertyDefinitionsStream, Stream.of(""))
    }
}
