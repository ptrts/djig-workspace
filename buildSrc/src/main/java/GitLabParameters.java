import org.apache.commons.io.FileUtils;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPluginExtension;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;

public class GitLabParameters {
    public URI gitlabUri;
    public URI projectUri;
    public String username;
    public String password;

    public static GitLabParameters getGitLabParameters(Project project, String propertiesResourcePath) throws IOException, URISyntaxException {

        Project appProject = project.project(":app");

        JavaPluginExtension javaPluginExtension = appProject.getExtensions().findByType(JavaPluginExtension.class);

        File resourcesDir = javaPluginExtension
                .getSourceSets()
                .getByName("main")
                .getResources()
                .getSrcDirs()
                .stream()
                .findFirst()
                .orElseThrow();

        File localGitlabPropertiesFile = FileUtils.getFile(resourcesDir, propertiesResourcePath);
        Properties localGitlabProperties = new Properties();

        try (Reader reader = new FileReader(localGitlabPropertiesFile)) {
            localGitlabProperties.load(reader);
        }

        GitLabParameters gitLabParameters = new GitLabParameters();
        String propertiesProjectUrlStr = localGitlabProperties.getProperty("dynamic-impl.git-repository.url");

        gitLabParameters.username = localGitlabProperties.getProperty("dynamic-impl.git-repository.username");
        gitLabParameters.password = localGitlabProperties.getProperty("dynamic-impl.git-repository.password");

        gitLabParameters.projectUri = new URI(propertiesProjectUrlStr);
        gitLabParameters.gitlabUri = new URI(
            gitLabParameters.projectUri.getScheme(),
            gitLabParameters.projectUri.getAuthority(),
            null,
            null,
            null
        );

        return gitLabParameters;
    }
}
