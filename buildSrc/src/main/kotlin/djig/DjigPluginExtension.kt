package djig

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import java.net.URL
import javax.inject.Inject

abstract class DjigPluginExtension(protected val project: Project) {

    @get:Inject
    protected abstract val objectFactory: ObjectFactory

    val gitLab: GitLab

    init {
        gitLab = objectFactory.newInstance(GitLab::class.java)
    }

    fun gitLab(configureAction: Action<GitLab>) {
        configureAction.execute(gitLab)
    }

    interface GitLab {
        val url: Property<URL>
        val username: Property<String>
        val password: Property<String>
    }
}
