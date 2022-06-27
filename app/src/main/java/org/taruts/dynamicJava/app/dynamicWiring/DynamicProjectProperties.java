package org.taruts.dynamicJava.app.dynamicWiring;

import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.properties.ConstructorBinding;
import org.taruts.gitUtils.GitRemoteUrlParser;

import java.io.File;
import java.nio.file.Path;

@Getter
public final class DynamicProjectProperties {
    final String url;
    final String username;
    final String password;
    final String dynamicInterfacePackage;
    final File localGitRepoDirectory;

    @ConstructorBinding
    public DynamicProjectProperties(
            String url,
            String username,
            String password,
            String dynamicInterfacePackage,
            String localGitRepoDirectoryName
    ) {
        this.url = url;
        this.username = username;
        this.password = password;
        this.dynamicInterfacePackage = dynamicInterfacePackage;

        if (StringUtils.isBlank(localGitRepoDirectoryName)) {
            GitRemoteUrlParser.Result urlParts = GitRemoteUrlParser.parse(url);
            localGitRepoDirectoryName = urlParts.projectName();
        }
        this.localGitRepoDirectory = Path.of(localGitRepoDirectoryName).toAbsolutePath().normalize().toFile();
    }
}
