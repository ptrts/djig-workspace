package workspace

import org.gradle.api.Project
import org.gradle.api.provider.Property
import java.util.stream.Collectors

abstract class WorkspacePluginExtension(private val project: Project) {

    abstract val workspaceRepositoryRelativePath: Property<String>

    fun workspaceProject(repositoryPostfix: String) {
        workspaceProject(repositoryPostfix, repositoryPostfix)
    }

    fun workspaceProject(repositoryPostfix: String, directoryRelativePath: String) {
        val parts: List<String> = directoryRelativePath.split("[-/]".toRegex())

        val name = parts
            .stream()
            .map { it.lowercase().replaceFirstChar(Char::titlecase) }
            .collect(Collectors.joining())
            .replaceFirstChar(Char::lowercase)

        workspaceProject(name, repositoryPostfix, directoryRelativePath)
    }

    fun workspaceProject(name: String, repositoryPostfix: String, directoryRelativePath: String) {
        val taskNamePostfix = name.replaceFirstChar(Char::titlecase)
        val taskName = "clone$taskNamePostfix"
        val taskProvider = project.tasks.register(taskName, CloneAdjacentGitRepositoryTask::class.java) {
            it.workspaceRepositoryRelativePath = this@WorkspacePluginExtension.workspaceRepositoryRelativePath.get()
            it.adjacentRepositoryPostfix = repositoryPostfix
            it.directoryRelativePath = directoryRelativePath
        }
        project.tasks.named("cloneAll").configure {
            it.dependsOn(taskProvider)
        }
    }
}
