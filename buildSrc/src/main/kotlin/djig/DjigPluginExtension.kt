package djig

import gitlabContainer.GitLabContainerPluginExtension
import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import java.net.URL
import javax.inject.Inject

open class DjigPluginExtension @Inject constructor(private val objectFactory: ObjectFactory) {

    val localGitLabsCreation: LocalGitLabsCreation = objectFactory.newInstance(LocalGitLabsCreation::class.java)

    fun localGitLabsCreation(configure: Action<LocalGitLabsCreation>) {
        configure.execute(localGitLabsCreation)
    }

    interface LocalGitLabsCreation {
        val sourceSpringBootProfile: Property<String>
        val targetGitLabs: LocalGitLabs
    }

    interface LocalGitLab {

        companion object {
            val GIT_LAB_CONTAINER_NAME = "gitLabContainer"
        }

        val name: Property<String>
        val springBootProfile: Property<String>
        val directoryPrefix: Property<String>
        val url: Property<URL>
        val username: Property<String>
        val password: Property<String?>

        // todo Кроме этого, сюда возможно стоит добавить еще вот что
        //          Префикс группы
        //          Замена группы
        //          Постфикс группы

        val isGitLabContainer: Boolean
            get() = GIT_LAB_CONTAINER_NAME.equals(name.get())
    }

    abstract class LocalGitLabs @Inject constructor(private val project: Project) : NamedDomainObjectContainer<LocalGitLab> {

        fun fromGitLabContainer(springBootProfile: String, directoryPrefix: String) {
            val gitLabContainerPluginExtension: GitLabContainerPluginExtension = project.extensions.getByType(
                GitLabContainerPluginExtension::class.java
            )

            register(
                name = LocalGitLab.GIT_LAB_CONTAINER_NAME,
                springBootProfile = springBootProfile,
                directoryPrefix = directoryPrefix,
                url = gitLabContainerPluginExtension.url.get(),
                username = gitLabContainerPluginExtension.username.get(),
                password = gitLabContainerPluginExtension.password.get()
            )
        }

        fun register(name: String, springBootProfile: String, directoryPrefix: String, url: URL, username: String, password: String? = null) {
            register(name) { newTargetGitLab ->
                newTargetGitLab.name.set(name)
                newTargetGitLab.directoryPrefix.set(directoryPrefix)
                newTargetGitLab.url.set(url)
                newTargetGitLab.username.set(username)
                newTargetGitLab.password.set(password)
                newTargetGitLab.springBootProfile.set(springBootProfile)
            }
        }
    }
}
