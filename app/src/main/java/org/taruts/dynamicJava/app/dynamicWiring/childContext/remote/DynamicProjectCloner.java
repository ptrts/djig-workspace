package org.taruts.dynamicJava.app.dynamicWiring.childContext.remote;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.taruts.dynamicJava.app.dynamicWiring.childContext.source.DynamicProjectLocalGitRepo;
import org.taruts.gitUtils.GitUtils;

import java.io.File;
import java.nio.file.Path;
import java.util.stream.Stream;

@Component
public class DynamicProjectCloner {

    @Autowired
    private CloneRetryTemplate cloneRetryTemplate;

    public DynamicProjectLocalGitRepo cloneWithRetries(DynamicProjectGitRemote remote, File projectSourceDirectory) {
        String url = tweakLocalDirectoryRemoteUrl(remote.url());
        return cloneRetryTemplate.execute(retryContext -> {
            GitUtils.cloneOrUpdate(url, remote.username(), remote.password(), projectSourceDirectory);
            return new DynamicProjectLocalGitRepo(remote, projectSourceDirectory);
        });
    }

    private String tweakLocalDirectoryRemoteUrl(String urlArg) {
        boolean isRemoteLocalDirectoryPath = Stream.of(
                "http:", "https:", "file:", "git@"
        ).noneMatch(urlArg::startsWith);

        if (isRemoteLocalDirectoryPath) {
            // Add "file://" so that cloning would be done in a similar way as with remotes on the network.
            // Plus, transform the path from relative to absolute.
            return "file://" + Path.of(urlArg).toAbsolutePath().normalize();
        } else {
            return urlArg;
        }
    }
}
