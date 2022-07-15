package djig

import gitlabContainer.GitLabContainerPluginExtension
import org.gradle.api.Action
import org.gradle.api.Named
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.internal.AbstractNamedDomainObjectContainer
import org.gradle.api.internal.CollectionCallbackActionDecorator
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.internal.reflect.Instantiator
import java.net.URL
import javax.inject.Inject

open class DjigPluginExtension @Inject constructor(private val objectFactory: ObjectFactory) {

    val localGitLabsCreation: LocalGitLabsCreation = objectFactory.newInstance(LocalGitLabsCreation::class.java)

    fun localGitLabsCreation(appProjectDirectoryRelativePath: String, sourceSpringBootProfile: String, configure: Action<LocalGitLabsCreation>) {
        localGitLabsCreation.appProjectDirectoryRelativePath.set(appProjectDirectoryRelativePath)
        localGitLabsCreation.sourceSpringBootProfile.set(sourceSpringBootProfile)
        configure.execute(localGitLabsCreation)
    }

    abstract class LocalGitLabsCreation @Inject constructor(private val objectFactory: ObjectFactory) {
        abstract val appProjectDirectoryRelativePath: Property<String>
        abstract val sourceSpringBootProfile: Property<String>
        val targetGitLabs: LocalGitLabs = objectFactory.newInstance(LocalGitLabs::class.java)
    }

    abstract class LocalGitLab @Inject constructor(val _name: String): Named {

        companion object {
            val GIT_LAB_CONTAINER_NAME = "gitLabContainer"
        }

        override fun getName(): String = _name

        val isGitLabContainer: Boolean
            get() = GIT_LAB_CONTAINER_NAME.equals(name)

        abstract val springBootProfile: Property<String>
        abstract val directoryPrefix: Property<String>
        abstract val url: Property<URL>
        abstract val username: Property<String>
        abstract val password: Property<String?>

        // todo Кроме этого, сюда возможно стоит добавить еще вот что
        //          Префикс группы
        //          Замена группы
        //          Постфикс группы
    }

    open class LocalGitLabs
    @Inject
    constructor(
        private val project: Project,
        private val objectFactory: ObjectFactory,
        instantiator: Instantiator,
        callbackActionDecorator: CollectionCallbackActionDecorator
    ) : AbstractNamedDomainObjectContainer<LocalGitLab>(LocalGitLab::class.java, instantiator, callbackActionDecorator) {

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
                newTargetGitLab.directoryPrefix.set(directoryPrefix)
                newTargetGitLab.url.set(url)
                newTargetGitLab.username.set(username)
                newTargetGitLab.password.set(password)
                newTargetGitLab.springBootProfile.set(springBootProfile)
            }
        }

        override fun doCreate(name: String): LocalGitLab {
            return objectFactory.newInstance(LocalGitLab::class.java, name)
        }
    }
}
