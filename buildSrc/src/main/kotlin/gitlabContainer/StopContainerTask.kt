package gitlabContainer

import gitlabContainer.ContainerUtils
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.taruts.processUtils.ProcessRunner

open class StopContainerTask : DefaultTask() {

    init {
        group = "gitlab-container"
    }

    @TaskAction
    fun action() {
        // println("stopContainer. Start")
        if (ContainerUtils.containerExists()) {
            // println("containerExists() == true")

            @Suppress("UNUSED_VARIABLE")
            val output: String = ProcessRunner.runProcess(
                project.projectDir,
                "docker", "container", "stop", "gitlab.taruts.net"
            )

            // println("Docker output: $output")
        }
        // println("stopContainer. End")
    }
}
