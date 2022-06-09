package gitlabContainer

import gitlabContainer.utils.DockerShellRunner
import gitlabContainer.utils.MountPoints
import org.apache.commons.io.FileUtils
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.util.stream.Collectors
import java.util.stream.Stream

open class RemoveContainerDataTask : DefaultTask() {

    init {
        group = "gitlab-container"
        mustRunAfter("removeContainer")
    }

    @TaskAction
    fun action() {
        val homeHostDirectory: File = FileUtils.getFile(project.projectDir, "home")
        if (!homeHostDirectory.exists()) {
            return
        }

        val directoryWildcards: String = Stream
            .of(MountPoints.CONFIG, MountPoints.LOGS, MountPoints.DATA)
            .map { mountPoint ->
                val visibleFilesWildcard = "$mountPoint/*"
                // <dot><not dot><etc>
                // .git - counts
                // . and .. - not
                val hiddenFilesWildcard = "$mountPoint/.[!.]*"
                return@map "$visibleFilesWildcard $hiddenFilesWildcard"
            }
            .collect(Collectors.joining(" "))
        val shellCommand = "rm -rf $directoryWildcards"
        DockerShellRunner.runLinuxShellCommandInDockerWithMounts(project, false, shellCommand)

        // Удаляем сами каталоги томов
        val homePath: String = FileUtils.getFile(project.projectDir, "home").canonicalPath
        val containerHomePath = "/tmp/pavel-taruts/gitlab-container/home"
        val bindMounts: List<String> = listOf(
            "--volume", "$homePath:$containerHomePath",
        )
        DockerShellRunner.runLinuxShellCommandInDocker(project, false, bindMounts, "rm -rf $containerHomePath/*")

        FileUtils.deleteDirectory(homeHostDirectory)
    }
}
