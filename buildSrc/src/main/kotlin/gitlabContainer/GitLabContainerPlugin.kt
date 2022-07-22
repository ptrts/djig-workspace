package gitlabContainer

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task

class GitLabContainerPlugin : Plugin<Project> {

    override fun apply(project: Project) {

        project.extensions.create<GitLabContainerPluginExtension>("gitLabContainer", GitLabContainerPluginExtension::class.java)

        project.tasks.register("gitLabContainerCreateContainer", CreateContainerTask::class.java)

        project.tasks.register("gitLabContainerCreateUser", CreateUserTask::class.java)

        project.tasks.register("gitLabContainerCreateAll") { task: Task ->
            task.group = "GitLab container"
            task.dependsOn("gitLabContainerCreateContainer", "gitLabContainerCreateUser")
        }

        project.tasks.register("gitLabContainerStartContainer", StartContainerTask::class.java)

        project.tasks.register("gitLabContainerStopContainer", StopContainerTask::class.java)

        project.tasks.register("gitLabContainerRemoveContainer", RemoveContainerTask::class.java)

        project.tasks.register("gitLabContainerRemoveContainerData", RemoveContainerDataTask::class.java)

        project.tasks.register("gitLabContainerRemoveAll") { task: Task ->
            task.group = "GitLab container"
            task.dependsOn("gitLabContainerRemoveContainer", "gitLabContainerRemoveContainerData")
        }
    }
}
