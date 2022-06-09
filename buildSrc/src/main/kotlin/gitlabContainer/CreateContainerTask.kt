package gitlabContainer

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
import reactor.util.Loggers
import java.io.File
import java.net.ConnectException
import java.time.Duration

open class CreateContainerTask : DefaultTask() {

    init {
        group = "gitlab-container"

        timeout.set(Duration.ofMinutes(15))
        dependsOn("removeAll")
    }

    @TaskAction
    fun action() {
        val home: File = FileUtils.getFile(project.projectDir, "home")
        FileUtils.getFile(home, "config").mkdirs()
        FileUtils.getFile(home, "logs").mkdirs()
        FileUtils.getFile(home, "data").mkdirs()

        val gitLabParameters: GitLabParameters =
            GitLabParameters.getGitLabParameters(project, "application-dynamic-local.properties")

        val command: MutableList<String> = mutableListOf()

        command.addAll(
            listOf(
                "docker", "run",
                "--detach",
                "--hostname", "gitlab.domain.name.placeholder",
                "--publish", "9522:22",
                "--publish", "${gitLabParameters.gitlabUri.port}:80",
                "--publish", "9543:443",
                "--name", "gitlab.taruts.net",
                "--restart", "always",
                "--shm-size", "256m"
            )
        )

        MountPoints.addBindMounts(command, project)

        if (SystemUtils.IS_OS_LINUX) {
            command.addAll(
                listOf(
                    "--add-host", "host.docker.internal:host-gateway"
                )
            )
        }

        command.add("gitlab/gitlab-ce:14.10.2-ce.0")

        ProcessRunner.runProcess(project.projectDir, command)

        Loggers.useSl4jLoggers()

        val httpClient: HttpClient = HttpClient
            .create()
            .compress(true)
            .baseUrl(gitLabParameters.gitlabUri.toString())

        val loggingManagerInternal: LoggingManagerInternal = logging as LoggingManagerInternal

        // reactor-netty-http spams warnings when throwing exceptions
        loggingManagerInternal.setLevelInternal(LogLevel.ERROR)

        while (true) {
            try {
                try {
                    val response: HttpClientResponse? = httpClient
                        .get()
                        .response()
                        .block(Duration.ofSeconds(10))
                    if (response != null) {
                        val code: Int = response.status().code()
                        if (code in 200..399) {
                            break
                        }
                    }
                } catch (e: Exception) {
                    // Unwrapping the cause if exists
                    throw if (e.cause == null) e else e.cause as Exception
                }
            } catch (ignored: PrematureCloseException) {
            } catch (ignored: ConnectException) {
            } catch (ignored: io.netty.channel.unix.Errors.NativeIoException) {
            } catch (e: Exception) {
                logger.error(">>> ${e::class.qualifiedName}: ${e.message}")
                throw e
            }
        }
    }
}
