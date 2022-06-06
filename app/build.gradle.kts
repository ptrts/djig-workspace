import org.apache.commons.io.FileUtils
import org.apache.commons.lang3.StringUtils
import org.gitlab4j.api.GitLabApi
import org.gitlab4j.api.GitLabApiException
import org.gitlab4j.api.models.Group
import org.taruts.gitUtils.GitUtils
import org.taruts.processUtils.ProcessRunner
import java.net.URI
import java.nio.charset.StandardCharsets
import java.nio.file.Path as JavaPath

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
        classpath("org.apache.commons:commons-lang3:3.12.0")
        classpath("commons-io:commons-io:2.11.0")
        classpath("com.google.guava:guava:31.1-jre")
        classpath("we:process-utils:001")
        classpath("we:git-utils:001")
        classpath("javax.inject:javax.inject:1")
        classpath("org.gitlab4j:gitlab4j-api:5.0.1")
    }
}

plugins {
    id("org.springframework.boot") version "2.6.4"
    id("io.spring.dependency-management") version "1.0.11.RELEASE"
    id("application")
}

group = "we.git-implementations"

java.sourceCompatibility = JavaVersion.VERSION_17

configurations {
    compileOnly {
        extendsFrom(configurations["annotationProcessor"])
    }
}

repositories {
    mavenLocal()
    mavenCentral()
    maven {
        name = "s3MavenRepo"
        url = uri("s3://maven.taruts.net")
        credentials(AwsCredentials::class)
    }
    mavenCentral()
}

dependencies {
    // Dev tools
    developmentOnly("org.springframework.boot:spring-boot-devtools")

    // Lombok
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    // Spring Boot + WebFlux
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    testImplementation("org.springframework.boot:spring-boot-starter-test")

    // Spring Retry
    implementation("org.springframework:spring-aspects")
    implementation("org.springframework.retry:spring-retry:1.2.5.RELEASE")

    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    implementation("we.git-implementations:dynamic-api:001")

    // GitLab API
    implementation("org.gitlab4j:gitlab4j-api:5.0.1")

    // Our libraries
    implementation("we:git-utils:001")
    implementation("we:gradle-utils:001")

    // Utils
    implementation("org.apache.commons:commons-lang3")
    implementation("commons-io:commons-io:2.11.0")
    implementation("com.google.guava:guava:31.1-jre")

    // Test
    testImplementation("io.projectreactor:reactor-test")
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}

tasks.register("initDynamicLocal") {
    group = "app"

    description = """
    Creates a fork of dynamic-dev in the local GitLab
    and clones it into the dynamic-local project subdirectory
    """

    doLast {
        // Вычисляем, куда мы будем клонироваться
        val dynamicLocalDirectory: File = JavaPath
            .of(rootProject.projectDir.path, "dynamic-local")
            .toAbsolutePath()
            .normalize()
            .toFile()
        // Удаляем этот каталог, если он есть
        if (dynamicLocalDirectory.exists()) {
            FileUtils.forceDelete(dynamicLocalDirectory)
        }
        FileUtils.forceMkdir(dynamicLocalDirectory)

        // Из основного проекта берем URL репозитория dynamic-dev
        val devGitLabParameters: GitLabParameters =
            GitLabParameters.getGitLabParameters(project, "application-dynamic-dev.properties")

        // Клонируемся в каталог build/dynamic-impl-dev
        GitUtils.clone(devGitLabParameters.projectUri.toString(), dynamicLocalDirectory)

        // Ставим пользователя и его мейл
        ProcessRunner.runProcess(dynamicLocalDirectory, "git", "config", "--local", "user.name", "user")
        ProcessRunner.runProcess(dynamicLocalDirectory, "git", "config", "--local", "user.email", "user@mail.com")

        // Изменяем имя проекта и коммитим это
        val settingsGradleFile: File = FileUtils.getFile(dynamicLocalDirectory, "settings.gradle.kts")
        FileUtils.writeStringToFile(settingsGradleFile, "rootProject.name = \"dynamic-local\"", StandardCharsets.UTF_8)
        ProcessRunner.runProcess(dynamicLocalDirectory, "git", "add", "*")
        ProcessRunner.runProcess(dynamicLocalDirectory, "git", "commit", "-m", "Changing project name to dynamic-local")

        val localGitLabParameters: GitLabParameters =
            GitLabParameters.getGitLabParameters(project, "application-dynamic-local.properties")

        // Create a GitLabApi instance to communicate with your GitLab server
        val gitLabApi: GitLabApi = GitLabApi.oauth2Login(
            localGitLabParameters.gitlabUri.toString(),
            localGitLabParameters.username,
            localGitLabParameters.password,
            true
        )

        val pathParts: List<String> = localGitLabParameters.projectUri.path
            .split("/")
            .stream()
            .filter(StringUtils::isNotBlank)
            .toList()
        val projectFileName: String = pathParts.get(pathParts.size - 1)

        val dotIndex: Int = projectFileName.lastIndexOf(".")
        val projectName: String =
            if (dotIndex == -1)
                projectFileName
            else
                projectFileName.substring(0, dotIndex)

        val groupPath: String = pathParts.subList(0, pathParts.size - 1).joinToString("/")

        // Ищем или создаем группу проектов
        val group: Group = gitLabApi.getGroupApi().getOptionalGroup(groupPath)
            .orElseGet {
                gitLabApi.getGroupApi().addGroup("Dynamic Java code stored in Git", groupPath)
            }

        var gitlabProject: org.gitlab4j.api.models.Project? = null

        // Ищем или создаем проект
        gitLabApi.getProjectApi().getOptionalProject(groupPath, projectName)
            .ifPresent {
                gitLabApi.getProjectApi().deleteProject(it.id)
                println("Waiting for project deletion...")
                var i: Int = 60
                while (i > 0) {
                    gitlabProject = gitLabApi.getProjectApi().getOptionalProject(groupPath, projectName).orElse(null)
                    if (gitlabProject == null) {
                        break
                    }
                    println("$i...")
                    Thread.sleep(1000)
                    i--
                }
                if (i == 0) {
                    throw RuntimeException("Could not delete project $projectName")
                }
            }

        for (i in 100 downTo 1) {
            try {
                gitlabProject = gitLabApi.getProjectApi().createProject(group.id, projectName)
                break
            } catch (e: GitLabApiException) {
                if (i == 1) {
                    throw e
                } else {
                    e.validationErrors.forEach { key, value ->
                        println("Gitlab project creation error: attempt $i, key = $key, value = $value")
                    }
                    Thread.sleep(1000)
                }
            }
        }

        assert(gitlabProject != null)

        var remoteUrlStr: String = gitlabProject!!.getHttpUrlToRepo()
        var remoteUri: URI = URI(remoteUrlStr)

        // Добавляем логин и пароль. Меняем хост и порт
        remoteUri = URI(
            remoteUri.scheme,
            "${localGitLabParameters.username}:${localGitLabParameters.password}",
            localGitLabParameters.gitlabUri.host,
            localGitLabParameters.gitlabUri.port,
            remoteUri.path,
            null,
            null
        )

        remoteUrlStr = remoteUri.toString()

        // Перенаправляем remote origin на наш новый репозиторий
        ProcessRunner.runProcess(dynamicLocalDirectory, "git", "remote", "set-url", "origin", remoteUrlStr)

        // Пушим все в новый репозиторий
        ProcessRunner.runProcess(dynamicLocalDirectory, "git", "push", "origin", "master")
    }
}

abstract class InitAdjacentGitRepoTask : DefaultTask {

    companion object {
        private final val STARTING_POINT_REPO_POSTFIX: String = "main"
    }


    @Input
    final val adjacentRepoPostfix: String

    @Inject
    constructor(adjacentRepoPostfix: String) {
        this.adjacentRepoPostfix = adjacentRepoPostfix

        this.group = "app"
        this.description = "Clones the $adjacentRepoPostfix project into the project subdirectory with the same name"
    }

    @TaskAction
    fun action() {
        // Определяем что клонировать. Вычисляем URL репозитория для клонирования, отталкиваясь от URL текущего репозитория
        val startingPointRepoUrl: String =
            ProcessRunner.runProcess(project.projectDir, "git", "remote", "get-url", "origin")

        // В URL нашего Git репозитория, фрагмент STARTING_POINT_REPO_POSTFIX в конце, перед ".git" заменяем на другое
        val adjacentRepoUrl: String = startingPointRepoUrl.replace(
            "$STARTING_POINT_REPO_POSTFIX(?=\\.git$)".toRegex(),
            adjacentRepoPostfix
        )

        // Определяем куда клонировать. Нацеливаемся на каталог, где у нас будут лежать исходники
        val sourceDir: File = FileUtils.getFile(project.rootDir, adjacentRepoPostfix)

        GitUtils.forceClone(adjacentRepoUrl, sourceDir)
    }
}

tasks.register("initDynamicApi", InitAdjacentGitRepoTask::class, "dynamic-api")
tasks.register("initDynamicDev", InitAdjacentGitRepoTask::class, "dynamic-dev")

tasks.register("initLocalGitLab") {
    group = "app"

    description = """
    Creates a local GitLab Docker container, 
    creates in there a project which is a fork of dynamic-dev 
    and clones it into the dynamic-local project subdirectory
    """

    dependsOn(":gitlab-container:createAll", "initDynamicLocal")
}

tasks.named("initDynamicLocal").configure {
    shouldRunAfter(":gitlab-container:createAll")
}

tasks.register("initProject") {
    group = "app"

    description = """
    Initializes everything for the project. 
    This is an aggregate task for initLocalGitLab, initDynamicApi and initDynamicDev
    """

    dependsOn("initLocalGitLab", "initDynamicApi", "initDynamicDev")
}

tasks.named("initDynamicApi").configure {
    shouldRunAfter("initLocalGitLab")
}

tasks.named("initDynamicDev").configure {
    shouldRunAfter("initLocalGitLab")
}
