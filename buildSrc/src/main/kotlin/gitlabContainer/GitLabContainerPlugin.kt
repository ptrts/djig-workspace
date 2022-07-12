package gitlabContainer

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task

class GitLabContainerPlugin : Plugin<Project> {

    override fun apply(project: Project) {

        project.extensions.create<GitLabContainerPluginExtension>("gitLabContainer", GitLabContainerPluginExtension::class.java)

        project.tasks.register("createContainer", CreateContainerTask::class.java)

        project.tasks.register("createUser", CreateUserTask::class.java)

        project.tasks.register("createAll") { task: Task ->
            task.group = "gitlab-container"
            task.dependsOn("createContainer", "createUser")
        }

        project.tasks.register("startContainer", StartContainerTask::class.java)

        project.tasks.register("stopContainer", StopContainerTask::class.java)

        project.tasks.register("removeContainer", RemoveContainerTask::class.java)

        project.tasks.register("removeContainerData", RemoveContainerDataTask::class.java)

        project.tasks.register("removeAll") { task: Task ->
            task.group = "gitlab-container"
            task.dependsOn("removeContainer", "removeContainerData")
        }
    }
}
