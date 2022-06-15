package gitlabContainer

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.taruts.processUtils.ProcessRunner

open class StartContainerTask : DefaultTask() {

    init {
        group = "gitlab-container"
    }

    @TaskAction
    fun action() {
        ProcessRunner.runProcess(
            project.projectDir,
            "docker", "container", "start", "gitlab.taruts.org"
        )
    }
}
