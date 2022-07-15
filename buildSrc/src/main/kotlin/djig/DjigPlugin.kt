package djig

import org.apache.commons.lang3.StringUtils
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider

class DjigPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        val extension: DjigPluginExtension = project.extensions.create<DjigPluginExtension>("djig", DjigPluginExtension::class.java)

        val hasContainerGitLabTask = object {
            var value: Boolean = false
        }

        val sourceSpringBootProfile = extension.localGitLabsCreation.sourceSpringBootProfile.get()
        val appProjectDirectoryRelativePath = extension.localGitLabsCreation.appProjectDirectoryRelativePath.get()

        val taskNames: List<String> = extension.localGitLabsCreation.targetGitLabs.map { targetGitLab ->
            val targetGitLabName: String = targetGitLab.name.get()
            val taskNamePostfix: String = StringUtils.capitalize(targetGitLabName)
            val taskName: String = "initLocalDynamicProjectsFor${taskNamePostfix}"

            val taskProvider: TaskProvider<InitLocalDynamicProjectsTask> = project.tasks.register(
                taskName,
                InitLocalDynamicProjectsTask::class.java,
                appProjectDirectoryRelativePath,
                sourceSpringBootProfile,
                targetGitLab
            )

            if (targetGitLab.isGitLabContainer) {
                hasContainerGitLabTask.value = true
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

        project.tasks.register("initAll") {
            it.group = "djig"
            it.description = """
            An aggregator tasks initializing everything for the workspace.
            """.trimIndent()
            it.dependsOn("initLocalDynamicProjects", "cloneAll")
        }

        project.tasks.named("cloneAll") {
            it.mustRunAfter("initLocalDynamicProjects")
        }

        // If one of the tasks creates projects in a local container GitLab created by gitlab-container-plugin,
        // then add the container initialization to initAll and make it run before all other tasks that initAll aggregates.
        if (hasContainerGitLabTask.value) {

            // Add the container initialization to initAll
            project.tasks.named("initAll") {
                it.dependsOn("gitLabContainerCreateAll")
            }

            // Make all other tasks in initAll run after the container is initialized
            project.tasks.named("cloneAll") {
                it.mustRunAfter("gitLabContainerCreateAll")
            }
            project.tasks.named("initLocalDynamicProjects") {
                it.mustRunAfter("gitLabContainerCreateAll")
            }
        }
    }
}
