import djig.DjigPlugin
import djig.DjigPluginExtension
import gitlabContainer.GitLabContainerPlugin
import gitlabContainer.GitLabContainerPluginExtension
import workspace.WorkspacePlugin
import workspace.WorkspacePluginExtension

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

apply<WorkspacePlugin>()
configure<WorkspacePluginExtension> {
    workspaceRepositoryRelativePath.set("workspace")
    workspaceProject("example/app", "example-app")
    workspaceProject("example/dynamic-api", "example-dynamic-api")
    workspaceProject("example/dynamic-dev", "example-dynamic-dev")
    workspaceProject("spring-boot-starter", "djig-spring-boot-starter")
    workspaceProject("dynamic-api", "djig-dynamic-api")
}
