import djig.DjigPlugin
import djig.DjigPluginExtension
import gitlabContainer.GitLabContainerPlugin
import gitlabContainer.GitLabContainerPluginExtension
import workspace.CloneAdjacentGitRepoTask

apply<GitLabContainerPlugin>()
configure<GitLabContainerPluginExtension> {
    url.set(java.net.URL("http://localhost:9580"))
    username.set("user")
    password.set("123456789")
}

apply<DjigPlugin>()
configure<DjigPluginExtension> {
    localGitLabsCreation("projects/example-app", "dynamic-dev") {
        targetGitLabs.fromGitLabContainer("dynamic-local", "dynamic-local-")
    }
}

tasks.register("cloneExampleApp", CloneAdjacentGitRepoTask::class) {
    adjacentRepoPostfix = "example/app"
    directoryRelativePath = "example-app"
}

tasks.register("cloneExampleDynamicApi", CloneAdjacentGitRepoTask::class) {
    adjacentRepoPostfix = "example/dynamic-api"
    directoryRelativePath = "example-dynamic-api"
}

tasks.register("cloneExampleDynamicDev", CloneAdjacentGitRepoTask::class) {
    adjacentRepoPostfix = "example/dynamic-dev"
    directoryRelativePath = "example-dynamic-dev"
}

tasks.register("cloneDjigSpringBootStarter", CloneAdjacentGitRepoTask::class) {
    adjacentRepoPostfix = "spring-boot-starter"
    directoryRelativePath = "djig-spring-boot-starter"
}

tasks.register("cloneDjigDynamicApi", CloneAdjacentGitRepoTask::class) {
    adjacentRepoPostfix = "dynamic-api"
    directoryRelativePath = "djig-dynamic-api"
}

tasks.register("cloneAll") {
    group = "workspace"
    dependsOn("cloneExampleDynamicApi", "cloneExampleDynamicDev", "cloneDjigSpringBootStarter", "cloneDjigDynamicApi")
}
