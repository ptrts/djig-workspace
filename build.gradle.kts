import org.taruts.djigGradlePlugin.DjigPluginExtension
import org.taruts.gitLabContainerGradlePlugin.GitLabContainerPluginExtension
import org.taruts.workspaceGradlePlugin.WorkspacePluginExtension
import java.net.URL

plugins {
    id("org.taruts.workspace") version "1.0.0"
    id("org.taruts.gitlab-container") version "1.0.0"
    id("org.taruts.djig") version "1.0.0"
}

configure<GitLabContainerPluginExtension> {
    url.set(URL("http://localhost:9580"))
    username.set("user")
    password.set("123456789")
}

configure<DjigPluginExtension> {
    localGitLabsCreation("projects/example-app", "dynamic-dev") {
        targetGitLabs.fromGitLabContainer("dynamic-local", "dynamic-local-")
    }
}

configure<WorkspacePluginExtension> {
    workspaceRepositoryRelativePath.set("workspace")
    workspaceProject("example/app", "example-app")
    workspaceProject("example/dynamic-api", "example-dynamic-api")
    workspaceProject("example/dynamic-dev", "example-dynamic-dev")
    workspaceProject("example/dynamic-dev-kotlin", "example-dynamic-dev-kotlin")
    workspaceProject("spring-boot-starter", "djig-spring-boot-starter")
    workspaceProject("dynamic-api", "djig-dynamic-api")
}
