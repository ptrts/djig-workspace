package app

import org.apache.commons.io.FileUtils
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.taruts.gitUtils.GitUtils
import org.taruts.processUtils.ProcessRunner
import java.io.File
import javax.inject.Inject

/**
 * This task clones another Git repo of the same group as this project itself into a subdirectory.
 * The project which uses this plagin is considered a workspace project, it's only purpose is to group other projects together.
 * You clone this workspace project first and you do it manually.
 * Other projects, which the workspace has are cloned to subdirectories.
 * They are cloned automatically, each by a separate instance of this task.
 * The subdirectories of the supporting projects should be specified in the .gitignore of the workspace project.
 * Remote Git repo URLs of the workspace project and containing projects are different only in their postfixes
 * which go just before the ".git" in the end.
 *
 * @param adjacentRepoPostfix
 * Git repo URL postfix of the cloned project.
 *
 * @param directoryRelativePath
 * The subdirectory inside the workspace project to clone the other project to.
 * If not specified then the default equal to [adjacentRepoPostfix] is assumed.
 */
abstract class CloneAdjacentGitRepoTask @Inject constructor(
    @Input val adjacentRepoPostfix: String,
    @Input val directoryRelativePath: String = adjacentRepoPostfix
) : DefaultTask() {

    companion object {
        /**
         * Everything that goes before this and the ".git" afterwards is the same for this main projects and the supporting ones
         */
        // todo Сие нужно чтобы как-то можно было указать в параметрах. Не всегда воркспейсовый проект у нас будет называться workspace
        private const val MAIN_PROJECT_REPO_POSTFIX: String = "workspace"
    }

    init {
        group = "workspace"
        description = "Clones the $adjacentRepoPostfix project into a project subdirectory ${directoryRelativePath}"
    }

    @TaskAction
    fun action() {
        // Get the remote URL of the main project
        val startingPointRepoUrl: String =
            ProcessRunner.runProcess(project.projectDir, "git", "remote", "get-url", "origin")

        // Replacing the postfix of the main project with the postfix of the one being cloned
        val adjacentRepoUrl: String = startingPointRepoUrl.replace(
            "$MAIN_PROJECT_REPO_POSTFIX(?=\\.git$)".toRegex(),
            adjacentRepoPostfix
        )

        // Determining the subdirectory to clone to
        val sourceDir: File = FileUtils.getFile(project.rootDir, directoryRelativePath)

        GitUtils.forceClone(adjacentRepoUrl, sourceDir)
    }
}
