package org.taruts.dynamicJava.app.dynamicWiring.childContext.remote;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.taruts.gitUtils.GitUtils;

import java.io.File;
import java.nio.file.Path;
import java.util.stream.Stream;

@Component
public class DynamicProjectCloner {

    @Autowired
    private CloneRetryTemplate cloneRetryTemplate;

    public void cloneWithRetries(DynamicProjectGitRemote remote, File projectSourceDirectory) {
        String url = tweakRemoteUrlIfLocalDirectory(remote.url());
        cloneRetryTemplate.execute(retryContext -> {
            GitUtils.cloneOrUpdate(url, remote.username(), remote.password(), projectSourceDirectory);
            return null;
        });
    }

    private String tweakRemoteUrlIfLocalDirectory(String urlArg) {
        if (isRemoteLocalDirectory(urlArg)) {
            // Add "file://" so that cloning would be done in a similar way as with remotes on the network.
            // Plus, transform the path from relative to absolute.
            return "file://" + Path.of(urlArg).toAbsolutePath().normalize();
        } else {
            return urlArg;
        }
    }

    private boolean isRemoteLocalDirectory(String urlArg) {
        return Stream.of(
                "http:", "https:", "file:", "git@"
        ).noneMatch(urlArg::startsWith);
    }
}
