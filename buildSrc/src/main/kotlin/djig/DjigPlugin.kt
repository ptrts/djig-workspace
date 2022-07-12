package djig

import gitlabContainer.GitLabContainerPluginExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

class DjigPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        val djigPluginExtension = project.extensions.create<DjigPluginExtension>("djig", DjigPluginExtension::class.java)
        setPropertiesFromGitLabContainerIfPossible(project, djigPluginExtension)
    }

    private fun setPropertiesFromGitLabContainerIfPossible(project: Project, djigPluginExtension: DjigPluginExtension) {

        if (!classExists("gitlabContainer.GitLabContainerPluginExtension")) {
            return
        }

        val gitLabContainerPluginExtension: GitLabContainerPluginExtension = project.extensions.getByType(
            GitLabContainerPluginExtension::class.java
        )

        val url = gitLabContainerPluginExtension.url.get()
        val username = gitLabContainerPluginExtension.username.get()
        val password = gitLabContainerPluginExtension.password.get()

        djigPluginExtension.gitLab.url.set(url)
        djigPluginExtension.gitLab.username.set(username)
        djigPluginExtension.gitLab.password.set(password)
    }

    private fun classExists(name: String): Boolean {
        try {
            Class.forName(name);
            return true;
        } catch (e: ClassNotFoundException) {
            return false;
        }
    }
}
