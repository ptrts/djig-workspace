package djig

import app.InitLocalDynamicProjectsTask
import org.apache.commons.lang3.StringUtils
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider

class DjigPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        val extension: DjigPluginExtension = project.extensions.create<DjigPluginExtension>("djig", DjigPluginExtension::class.java)

        val gitLabContainerTaskName = object {
            var value: String? = null
        }

        val sourceSpringBootProfile = extension.localGitLabsCreation.sourceSpringBootProfile.get()

        val taskNames: List<String> = extension.localGitLabsCreation.targetGitLabs.map { targetGitLab ->
            val targetGitLabName: String = targetGitLab.name.get()
            val taskNamePostfix: String = StringUtils.capitalize(targetGitLabName)
            val taskName: String = "initLocalDynamicProjectsFor${taskNamePostfix}"

            val taskProvider: TaskProvider<InitLocalDynamicProjectsTask> = project.tasks.register(
                taskName,
                InitLocalDynamicProjectsTask::class.java,
                sourceSpringBootProfile,
                targetGitLab
            )

            if (targetGitLab.isGitLabContainer) {
                gitLabContainerTaskName.value = taskName
                taskProvider.configure {
                    it.mustRunAfter("gitLabContainerCreateAll")
                }
            }

            taskName
        }

        project.tasks.register("initLocalDynamicProjects") {
            it.group = "djig"
            it.description = """
            Initializes dynamic projects from application-${sourceSpringBootProfile}.properties for all GitLabs.
            This is an aggregator task for these tasks: ${taskNames.joinToString(", ")}.
            """.trimIndent()
            it.dependsOn(*taskNames.toTypedArray())
        }

        if (gitLabContainerTaskName.value != null) {
            project.tasks.register("gitLabContainerCreateAllWithProjects") {
                it.group = "gitlab-container"
                it.dependsOn("gitLabContainerCreateAll", gitLabContainerTaskName.value)
            }
        }
    }
}
