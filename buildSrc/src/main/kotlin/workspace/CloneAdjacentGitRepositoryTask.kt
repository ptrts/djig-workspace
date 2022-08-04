package workspace

import org.apache.commons.io.FileUtils
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.taruts.gitUtils.GitUtils
import org.taruts.processUtils.ProcessRunner
import java.io.File

/**
 * This task clones another Git repository of the same group as this project itself into a subdirectory.
 * The project which uses this plagin is considered a workspace project, it's only purpose is to group other projects together.
 * You clone this workspace project first and you do it manually.
 * Other projects, which the workspace has are cloned to subdirectories.
 * They are cloned automatically, each by a separate instance of this task.
 * The subdirectories of the supporting projects should be specified in the .gitignore of the workspace project.
 * Remote Git repository URLs of the workspace project and containing projects are different only in their postfixes
 * which go just before the ".git" in the end.
 *
 * @param adjacentRepositoryPostfix
 * Git repository URL postfix of the cloned project.
 *
 * @param directoryRelativePath
 * The subdirectory inside the workspace project to clone the other project to.
 * If not specified then the default equal to [adjacentRepositoryPostfix] is assumed.
 */
abstract class CloneAdjacentGitRepositoryTask : DefaultTask() {

    /**
     * This is how we determine the parent path that is common for the workspace project and projects that are parts of it (adjacent
     * projects).
     * All relative repository paths are relative to this common parent path.
     * We extract the parent part from the workspace repository path by specifying its "child" part in the end of the full path.
     */
    @Input
    var workspaceRepositoryRelativePath: String? = null

    @Input
    var adjacentRepositoryPostfix: String? = null
        set(value) {
            field = value
            updateDescription()
        }

    @Input
    var directoryRelativePath: String? = null
        set(value) {
            field = value
            updateDescription()
        }

    init {
        group = "workspace"
    }

    private fun updateDescription() {
        description = "Clones the $adjacentRepositoryPostfix project into a project subdirectory projects/$directoryRelativePath"
    }

    @TaskAction
    fun action() {
        val workspaceRepositoryRelativePath = this.workspaceRepositoryRelativePath!!.replace("\\.git$".toRegex(), "")
        val adjacentRepositoryPostfix: String = this.adjacentRepositoryPostfix!!
        val directoryRelativePath: String = this.directoryRelativePath!!

        // Get the remote URL of the main project
        val workspaceRepositoryUrl: String =
            ProcessRunner.runProcess(project.projectDir, "git", "remote", "get-url", "origin")

        // Replacing the postfix of the main project with the postfix of the one being cloned
        val adjacentRepositoryUrl: String = workspaceRepositoryUrl.replace(
            "$workspaceRepositoryRelativePath(?=\\.git$)".toRegex(),
            adjacentRepositoryPostfix
        )

        // Determining the subdirectory to clone to
        val sourceDir: File = FileUtils.getFile(project.rootDir, "projects", directoryRelativePath)

        GitUtils.forceClone(adjacentRepositoryUrl, sourceDir)
    }
}
