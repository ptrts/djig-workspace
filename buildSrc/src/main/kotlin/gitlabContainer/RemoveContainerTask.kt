package gitlabContainer

import gitlabContainer.utils.ContainerUtils
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.taruts.processUtils.ProcessRunner

open class RemoveContainerTask : DefaultTask() {

    init {
        group = "gitlab-container"
        dependsOn("stopContainer")
    }

    @TaskAction
    fun action() {
        if (ContainerUtils.containerExists()) {
            ProcessRunner.runProcess(
                project.projectDir,
                "docker", "container", "rm", "--volumes", "gitlab.taruts.net"
            )
        }
    }
}
