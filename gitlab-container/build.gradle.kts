import gitlabContainer.GitLabParameters
import org.apache.commons.io.FileUtils
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.SystemUtils
import org.gitlab4j.api.GitLabApi
import org.gitlab4j.api.models.User
import org.gradle.internal.logging.LoggingManagerInternal
import org.taruts.processUtils.ProcessRunner
import reactor.netty.http.client.HttpClient
import reactor.netty.http.client.HttpClientResponse
import reactor.netty.http.client.PrematureCloseException
import reactor.util.Loggers
import java.net.ConnectException
import java.time.Duration
import java.util.regex.Pattern
import java.util.stream.Collectors
import java.util.stream.Stream

buildscript {
    repositories {
        mavenLocal()
        mavenCentral()
        maven {
            name = "s3MavenRepo"
            url = uri("s3://maven.taruts.net")
            credentials(AwsCredentials::class)
        }
    }
    dependencies {
        classpath("io.projectreactor.netty:reactor-netty-http:1.0.18")
        classpath("org.slf4j:slf4j-simple:1.7.36")
        classpath("org.apache.commons:commons-lang3:3.12.0")
        classpath("commons-io:commons-io:2.11.0")
        classpath("com.google.guava:guava:31.1-jre")
        classpath("we:process-utils:001")
        classpath("we:git-utils:001")
        classpath("javax.inject:javax.inject:1")
        classpath("org.gitlab4j:gitlab4j-api:5.0.1")
        classpath("io.netty:netty-transport-native-unix-common:4.1.74.Final")
    }
}

class MountPoints {
    companion object {
        const val CONFIG = "/etc/gitlab"
        const val LOGS = "/var/log/gitlab"
        const val DATA = "/var/opt/gitlab"
    }
}

fun addBindMounts(command: MutableList<String>, project: Project) {
    command.addAll(
        getBindMounts(project)
    )
}

fun getBindMounts(project: Project): List<String> {
    val home: String = FileUtils.getFile(project.projectDir, "home").canonicalPath
    return listOf(
        "--volume", "$home/config:${MountPoints.CONFIG}",
        "--volume", "$home/logs:${MountPoints.LOGS}",
        "--volume", "$home/data:${MountPoints.DATA}",
    )
}

tasks.register("createContainer") {
    group = "gitlab-container"
    timeout.set(Duration.ofMinutes(15))
    dependsOn("removeAll")
    doLast {

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

        addBindMounts(command, project)

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

tasks.register("createUser") {
    group = "gitlab-container"
    doLast {

        val fileContents: String =
            runLinuxShellCommandInDockerWithMounts(project, false, "cat ${MountPoints.CONFIG}/initial_root_password")

        val lines: List<String> = fileContents.split("\\n")

        val password: String = lines
            .stream()
            .map { line ->
                val pattern: Pattern = Pattern.compile("Password:\\s?(.+)")
                return@map pattern.matcher(line)
            }
            .filter { matcher -> matcher.find() }
            .map { matcher -> matcher.group(1) }
            .findAny()
            .orElseThrow { RuntimeException("Password not found") }

        val gitLabParameters: GitLabParameters =
            GitLabParameters.getGitLabParameters(project, "application-dynamic-local.properties")

        val gitLabApi: GitLabApi = GitLabApi.oauth2Login(gitLabParameters.gitlabUri.toString(), "root", password, true)

        val user = User()
        user.username = gitLabParameters.username
        user.name = gitLabParameters.username
        user.email = "${gitLabParameters.username}@mail.com"
        user.isAdmin = true
        user.canCreateGroup = true
        user.projectsLimit = 0
        user.sharedRunnersMinutesLimit = 0
        user.skipConfirmation = true

        gitLabApi.userApi.createUser(user, gitLabParameters.password, false)
    }
}

tasks.register("createAll") {
    group = "gitlab-container"
    dependsOn("createContainer", "createUser")
}

tasks.register("startContainer") {
    group = "gitlab-container"
    doLast {
        ProcessRunner.runProcess(
            project.projectDir,
            "docker", "container", "start", "gitlab.taruts.net"
        )
    }
}

fun containerExists(): Boolean {
    // println("containerExists")
    val containerId: String = ProcessRunner.runProcess(
        project.projectDir,
        "docker", "container", "ls",
        "-q",
        "-a",
        "-f", "name=gitlab.taruts.net"
    )
    // println("containerId=$containerId")
    return StringUtils.isNotBlank(containerId)
}

tasks.register("stopContainer") {
    group = "gitlab-container"
    doLast {
        // println("stopContainer. Start")
        if (containerExists()) {
            // println("containerExists() == true")

            @Suppress("UNUSED_VARIABLE")
            val output: String = ProcessRunner.runProcess(
                project.projectDir,
                "docker", "container", "stop", "gitlab.taruts.net"
            )

            // println("Docker output: $output")
        }
        // println("stopContainer. End")
    }
}

tasks.register("removeContainer") {
    group = "gitlab-container"
    dependsOn("stopContainer")
    doLast {
        if (containerExists()) {
            ProcessRunner.runProcess(
                project.projectDir,
                "docker", "container", "rm", "--volumes", "gitlab.taruts.net"
            )
        }
    }
}

fun runLinuxShellCommandInDockerWithMounts(project: Project, inheritIO: Boolean, shellCommand: String): String {
    val bindMounts: List<String> = getBindMounts(project)
    return runLinuxShellCommandInDocker(project, inheritIO, bindMounts, shellCommand)
}

fun runLinuxShellCommandInDocker(
    project: Project,
    inheritIO: Boolean,
    dockerRunOptions: List<String>,
    shellCommand: String
): String {
    val command: MutableList<String> = ArrayList()
    command.addAll(
        listOf(
            "docker", "run", "--rm"
        )
    )
    command.addAll(dockerRunOptions)
    command.add("alpine:3.16.0")
    command.addAll(
        listOf(
            "sh", "-c", shellCommand
        )
    )
    return ProcessRunner.runProcess(project.projectDir, inheritIO, command)
}

tasks.register("removeContainerData") {
    group = "gitlab-container"
    shouldRunAfter("removeContainer")
    doLast {
        val homeHostDirectory: File = FileUtils.getFile(project.projectDir, "home")
        if (!homeHostDirectory.exists()) {
            return@doLast
        }

        val directoryWildcards: String = Stream
            .of(MountPoints.CONFIG, MountPoints.LOGS, MountPoints.DATA)
            .map { mountPoint ->
                val visibleFilesWildcard = "$mountPoint/*"
                // <dot><not dot><etc>
                // .git - counts
                // . and .. - not
                val hiddenFilesWildcard = "$mountPoint/.[!.]*"
                return@map "$visibleFilesWildcard $hiddenFilesWildcard"
            }
            .collect(Collectors.joining(" "))
        val shellCommand = "rm -rf $directoryWildcards"
        runLinuxShellCommandInDockerWithMounts(project, false, shellCommand)

        // Удаляем сами каталоги томов
        val homePath: String = FileUtils.getFile(project.projectDir, "home").canonicalPath
        val containerHomePath = "/tmp/pavel-taruts/gitlab-container/home"
        val bindMounts: List<String> = listOf(
            "--volume", "$homePath:$containerHomePath",
        )
        runLinuxShellCommandInDocker(project, false, bindMounts, "rm -rf $containerHomePath/*")

        FileUtils.deleteDirectory(homeHostDirectory)
    }
}

tasks.register("removeAll") {
    group = "gitlab-container"
    dependsOn("removeContainer", "removeContainerData")
}
