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
 * This project is considered a main one, you clone it first manually.
 * Those cloned to subdirectories are kind of supporting projects.
 * They are cloned automatically, each by a separate instance of this task.
 * The subdirectories of the supporting projects should be specified in the .gitignore of this project.
 * Remote Git repo URLs of the main project and supporting ones are different only in their postfixes
 * which go just before the ".git" in the end.
 *
 * @param adjacentRepoPostfix   Git repo URL postfix of the cloned project
 */
abstract class CloneAdjacentGitRepoTask @Inject constructor(@Input val adjacentRepoPostfix: String) : DefaultTask() {

    companion object {
        /**
         * Everything that goes before this and the ".git" afterwards is the same for this main projects and the supporting ones
         */
        // todo Сие нужно чтобы как-то можно было указать в параметрах. Не всегда воркспейсовый проект у нас будет называться workspace
        private const val MAIN_PROJECT_REPO_POSTFIX: String = "workspace"
    }

    init {
        group = "workspace"
        description = "Clones the $adjacentRepoPostfix project into a project subdirectory with the same name"
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
        val sourceDir: File = FileUtils.getFile(project.rootDir, adjacentRepoPostfix)

        GitUtils.forceClone(adjacentRepoUrl, sourceDir)
    }
}
