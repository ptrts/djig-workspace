package gitlabContainer.utils

import org.gradle.api.Project
import org.taruts.processUtils.ProcessRunner

class DockerShellRunner {
    companion object {
        fun runLinuxShellCommandInDockerWithMounts(project: Project, inheritIO: Boolean, shellCommand: String): String {
            val bindMounts: List<String> = MountPoints.getBindMounts(project)
            return runLinuxShellCommandInDocker(project, inheritIO, bindMounts, shellCommand)
        }

        fun runLinuxShellCommandInDocker(
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
}
