package djig

import gitlabContainer.GitLabContainerPluginExtension
import org.gradle.api.Action
import org.gradle.api.Named
import org.gradle.api.Project
import org.gradle.api.internal.AbstractNamedDomainObjectContainer
import org.gradle.api.internal.CollectionCallbackActionDecorator
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Nested
import org.gradle.internal.reflect.Instantiator
import java.net.URL
import javax.inject.Inject

abstract class DjigPluginExtension {

    @get:Nested
    abstract val localGitLabsCreation: LocalGitLabsCreation

    fun localGitLabsCreation(
        appProjectDirectoryRelativePath: String,
        sourceSpringBootProfile: String,
        configure: Action<LocalGitLabsCreation>
    ) {
        localGitLabsCreation.appProjectDirectoryRelativePath.set(appProjectDirectoryRelativePath)
        localGitLabsCreation.sourceSpringBootProfile.set(sourceSpringBootProfile)
        configure.execute(localGitLabsCreation)
    }

    interface LocalGitLabsCreation {
        val appProjectDirectoryRelativePath: Property<String>
        val sourceSpringBootProfile: Property<String>

        @get:Nested
        val targetGitLabs: LocalGitLabs
    }

    interface LocalGitLab : Named {

        companion object {
            val GIT_LAB_CONTAINER_NAME = "gitLabContainer"
        }

        val springBootProfile: Property<String>
        val directoryPrefix: Property<String>
        val url: Property<URL>
        val username: Property<String>
        val password: Property<String?>

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
    ) : AbstractNamedDomainObjectContainer<LocalGitLab>(
        LocalGitLab::class.java,
        instantiator,
        callbackActionDecorator
    ) {
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

        fun register(
            name: String,
            springBootProfile: String,
            directoryPrefix: String,
            url: URL,
            username: String,
            password: String? = null
        ) {
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

// A workaround for adding a default interface method implementation in Kotlin
val DjigPluginExtension.LocalGitLab.isGitLabContainer: Boolean
    get() = DjigPluginExtension.LocalGitLab.GIT_LAB_CONTAINER_NAME.equals(name)
