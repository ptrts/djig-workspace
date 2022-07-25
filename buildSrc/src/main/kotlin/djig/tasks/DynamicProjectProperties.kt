package djig.tasks

import org.apache.commons.lang3.StringUtils
import java.net.URI
import java.net.URL

// todo Сие наверное надо тоже вынести в библиотеку или плагин Gradle, и отвязать от наших проектов "example-"
//      Может быть, этот класс нужно как-то объединить с теми пропертями, которые у нас используются в самом приложении

// todo Нужен нормальный KDoc. Не понятно, это только для парсинга файлов с пропертями, или это что-то более широкого назначения

/**
 * Note that the credentials ([username] and [password]) are those used for cloning only.
 * They are not supposed to be used for pushing.
 */
class DynamicProjectProperties(
    val gitlabUrl: URL,
    val projectUrl: URL,
    /**
     * Holds the username if the username+password authentication is used (oauth2).
     * In case of authentication with a personal/project/group access token or an impersonation token holds the token
     * (if so, [password] must be null).
     */
    val username: String?,
    val password: String?,
    val dynamicInterfacePackage: String
) {
    companion object {

        fun create(
            projectUrl: URL,
            username: String?,
            password: String?,
            dynamicInterfacePackage: String
        ): DynamicProjectProperties {
            val projectUri: URI = projectUrl.toURI()
            return DynamicProjectProperties(
                gitlabUrl = URI(
                    projectUri.scheme,
                    projectUri.authority,
                    null,
                    null,
                    null
                ).toURL(),
                projectUrl = projectUrl,
                username = username,
                password = password,
                dynamicInterfacePackage = dynamicInterfacePackage
            )
        }

        fun fromMap(projectPropertiesMap: MutableMap<String, String>): DynamicProjectProperties {
            //@formatter:off
            val propertiesProjectUrlStr : String  = projectPropertiesMap["url"]!!
            var username                : String?  = projectPropertiesMap["username"]
            var password                : String? = projectPropertiesMap["password"]
            val dynamicInterfacePackage : String  = projectPropertiesMap["dynamic-interface-package"]!!
            //@formatter:on

            // Replacing empty strings with nulls. For username null would lead to NPE
            username = StringUtils.defaultIfBlank(username, null)
            password = StringUtils.defaultIfBlank(password, null)

            val projectUrl = URL(propertiesProjectUrlStr)
            return create(projectUrl, username, password, dynamicInterfacePackage)
        }
    }

    fun toShortNameMap(): Map<String, String> {
        return mapOf(
            //@formatter:off
            "url"                       to projectUrl.toString(),
            "username"                  to (username ?: ""),
            "password"                  to (password ?: ""),
            "dynamic-interface-package" to dynamicInterfacePackage,
            //@formatter:on
        )
    }
}