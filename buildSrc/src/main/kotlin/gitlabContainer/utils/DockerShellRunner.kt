package gitlabContainer.utils

import org.gradle.api.Project
import org.taruts.processUtils.ProcessRunner

object DockerShellRunner {
    fun runCommandInDockerWithGitLabMounts(project: Project, inheritIO: Boolean, shellCommand: String): String {
        val bindMounts: List<String> = GitLabContainerMountPoints.getBindMounts(project)
        return runCommandInDocker(project, inheritIO, bindMounts, shellCommand)
    }

    fun runCommandInDocker(
        project: Project,
        inheritIO: Boolean,
        dockerRunOptions: List<String>,
        shellCommand: String
    ): String {
        val command: MutableList<String> = ArrayList()
        command.addAll(
            listOf(
                "docker", "run", "--rm"
            )
        )
        command.addAll(dockerRunOptions)
        command.add("alpine:3.16.0")
        command.addAll(
            listOf(
                "sh", "-c", shellCommand
            )
        )
        return ProcessRunner.runProcess(project.projectDir, inheritIO, command)
    }
}
