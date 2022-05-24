import org.apache.commons.io.FileUtils
import org.gradle.api.Project

class GitLabParameters {
    URI gitlabUri
    URI projectUri
    String username
    String password

    static GitLabParameters getGitLabParameters(Project project, String propertiesResourcePath) {
        String resourcesDir = project.project(':app').sourceSets.main.resources.srcDirs.first()
        File localGitlabPropertiesFile = FileUtils.getFile(resourcesDir, propertiesResourcePath)
        Properties localGitlabProperties = new Properties()
        try (Reader reader = localGitlabPropertiesFile.newReader()) {
            localGitlabProperties.load(reader)
        }

        GitLabParameters gitLabParameters = new GitLabParameters()
        String propertiesProjectUrlStr = localGitlabProperties.getProperty('dynamic-impl.git-repository.url')

        gitLabParameters.username = localGitlabProperties.getProperty('dynamic-impl.git-repository.username')
        gitLabParameters.password = localGitlabProperties.getProperty('dynamic-impl.git-repository.password')

        gitLabParameters.projectUri = new URI(propertiesProjectUrlStr)
        gitLabParameters.gitlabUri = new URI(
            gitLabParameters.projectUri.scheme,
            gitLabParameters.projectUri.authority,
            null,
            null,
            null
        )

        return gitLabParameters
    }
}
