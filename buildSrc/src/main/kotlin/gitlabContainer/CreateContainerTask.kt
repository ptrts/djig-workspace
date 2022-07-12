package gitlabContainer

import gitlabContainer.utils.GitLabContainerMountPoints
import io.netty.channel.unix.Errors
import org.apache.commons.io.FileUtils
import org.apache.commons.lang3.SystemUtils
import org.gradle.api.DefaultTask
import org.gradle.api.logging.LogLevel
import org.gradle.api.tasks.TaskAction
import org.gradle.internal.logging.LoggingManagerInternal
import org.taruts.processUtils.ProcessRunner
import reactor.netty.http.client.HttpClient
import reactor.netty.http.client.HttpClientResponse
import reactor.netty.http.client.PrematureCloseException
import java.io.File
import java.net.ConnectException
import java.net.URL
import java.time.Duration

open class CreateContainerTask : DefaultTask() {

    init {
        group = "gitlab-container"

        timeout.set(Duration.ofMinutes(15))
        dependsOn("removeAll")
    }

    @TaskAction
    fun action() {
        createVolumeDirectoriesOnHost()

        val gitLabContainerPluginExtension = project.extensions.findByType(
            GitLabContainerPluginExtension::class.java
        )!!

        val url: URL = gitLabContainerPluginExtension.url.get()

        createContainer(url)
        waitUntilContainerIsReady(url)
    }

    private fun createVolumeDirectoriesOnHost() {
        // These are host directories to be mounted inside the container.
        // We create them here ourselves to prevent Docker from doing so.
        // This is because if Docker creates them, they'll have different permitions,
        // which would make it harder to delete them when we decide to remove the container.
        val home: File = FileUtils.getFile(project.projectDir, "home")
        FileUtils.getFile(home, "config").mkdirs()
        FileUtils.getFile(home, "logs").mkdirs()
        FileUtils.getFile(home, "data").mkdirs()
    }

    private fun createContainer(url: URL) {
        val command: MutableList<String> = mutableListOf()

        command.addAll(
            listOf(
                "docker", "run",
                "--detach",
                "--hostname", "gitlab.domain.name.placeholder",
                "--publish", "9522:22",
                "--publish", "${url.port}:80",
                "--publish", "9543:443",
                "--name", "gitlab.taruts.org",
                "--restart", "always",
                "--shm-size", "256m"
            )
        )

        GitLabContainerMountPoints.addBindMounts(command, project)

        if (SystemUtils.IS_OS_LINUX) {
            command.addAll(
                listOf(
                    "--add-host", "host.docker.internal:host-gateway"
                )
            )
        }

        command.add("gitlab/gitlab-ce:14.10.2-ce.0")

        ProcessRunner.runProcess(project.projectDir, command)
    }

    private fun waitUntilContainerIsReady(url: URL) {
        val httpClient: HttpClient = HttpClient
            .create()
            .compress(true)
            .baseUrl(url.toString())

        val loggingManagerInternal: LoggingManagerInternal = logging as LoggingManagerInternal

        // reactor-netty-http spams warnings when throwing exceptions, so we use this dirty hack to
        // change the logging level just for this particular task
        loggingManagerInternal.setLevelInternal(LogLevel.ERROR)

        while (true) {
            try {
                try {
                    val response: HttpClientResponse = httpClient
                        .get()
                        .response()
                        .block(Duration.ofSeconds(10))!!
                    val code: Int = response.status().code()
                    if (code in 200..399) {
                        break
                    }
                } catch (e: Exception) {
                    // Unwrapping the cause if exists
                    throw e.cause ?: e
                }
            } catch (ignored: PrematureCloseException) {
            } catch (ignored: ConnectException) {
            } catch (ignored: Errors.NativeIoException) {
            }
        }
    }
}
