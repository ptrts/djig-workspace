package gitlabContainer

import org.gradle.api.provider.Property
import java.net.URL

interface GitLabContainerPluginExtension {
    val url: Property<URL>
    val username: Property<String>
    val password: Property<String>
}
