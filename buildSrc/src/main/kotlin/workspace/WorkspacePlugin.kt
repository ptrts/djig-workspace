package workspace

import org.gradle.api.Plugin
import org.gradle.api.Project

class WorkspacePlugin : Plugin<Project> {

    override fun apply(project: Project) {
        // An aggregator task for all cloneX tasks
        // All cloneX tasks will be added to the cloneAll task dependencies upon creation
        project.tasks.register("cloneAll") {
            it.group = "workspace"
        }
        project.extensions.create<WorkspacePluginExtension>("workspace", WorkspacePluginExtension::class.java, project)
    }
}
