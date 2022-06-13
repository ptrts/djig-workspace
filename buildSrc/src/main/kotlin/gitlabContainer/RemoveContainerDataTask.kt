package gitlabContainer

import gitlabContainer.utils.DockerShellRunner
import gitlabContainer.utils.GitLabContainerMountPoints
import org.apache.commons.io.FileUtils
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.util.stream.Collectors
import java.util.stream.Stream

open class RemoveContainerDataTask : DefaultTask() {

    init {
        group = "gitlab-container"

        description = """
        Removes the container data in host directories having been mounted inside the container.
        This task is supposed to be run after the container itself is deleted
        """.trimIndent()

        mustRunAfter("removeContainer")
    }

    @TaskAction
    fun action() {

        // Get the "home" directory as a File object
        val hostHomeDirectory: File = FileUtils.getFile(project.projectDir, "home")

        // If the "home" does not exist, then we don't need to remove everything, everything has been removed already.
        if (!hostHomeDirectory.exists()) {
            return
        }

        // Now we need to remove host directories that were mounted inside the GitLab container

        // First we remove the contents of those directories
        removeHostVolumeDirectoriesContents()

        // Then the directories
        removeHostVolumeDirectories(hostHomeDirectory)

        // Then the "home" directory that contains all those directories that were mounted inside the GitLab container
        FileUtils.deleteDirectory(hostHomeDirectory)
    }

    private fun removeHostVolumeDirectoriesContents() {

        // To remove the files inside the volume directories,
        // we need a wildcard that would match any file or directory inside a volume directory.
        // The wildcard must use paths inside the container.
        // For each of the container volume directories,
        // we make a wildcard for all files and directories directly inside the volume directory.
        // Then we join all wildcards for all the volume directories into one big wildcard.
        val directoryWildcards: String = Stream
            .of(GitLabContainerMountPoints.CONFIG, GitLabContainerMountPoints.LOGS, GitLabContainerMountPoints.DATA)
            .map { mountPoint ->
                val visibleFilesWildcard = "$mountPoint/*"
                // <dot><not dot><etc>
                // .git - counts
                // . and .. - not
                val hiddenFilesWildcard = "$mountPoint/.[!.]*"
                return@map "$visibleFilesWildcard $hiddenFilesWildcard"
            }
            .collect(Collectors.joining(" "))

        // Using the big wildcard to recursively remove all the files and directories that are directly inside a volume directory.
        val shellCommand = "rm -rf $directoryWildcards"
        DockerShellRunner.runCommandInDockerWithGitLabMounts(project, false, shellCommand)
    }

    private fun removeHostVolumeDirectories(hostHomeDirectory: File) {

        // We cannot remove the volume directories on the host with code running directly on the host.
        // Or we can, but only if the OS of the host is Windows.
        // We cannot if it's Linux.
        // If it's Linux, then during the execution of the GitLab container Docker has changed the owner to "root" or "docker"
        // and granted the write permission only to the owner ("root" or "docker").
        // But we still can remove those files if rather than doing it ourselves directly we ask Docker to do it.
        // This is a hack of some sort.
        // We run a minimal Docker container mounting the "home" directory inside.
        // This is different from when we run the GitLab container, where we mount subdirectories of "home" rather than "home" itself.
        // This way the subdirectories of "home" are not volume mount points,
        // they are just data inside a volume and so the container can delete them with a standard Linux command.

        val hostHomePath: String = hostHomeDirectory.canonicalPath
        val containerHomePath = "/tmp/pavel-taruts/gitlab-container/home"
        val bindMounts: List<String> = listOf(
            "--volume", "$hostHomePath:$containerHomePath",
        )
        DockerShellRunner.runCommandInDocker(project, false, bindMounts, "rm -rf $containerHomePath/*")
    }
}
