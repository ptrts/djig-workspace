package org.taruts.dynamicJava.app.dynamicWiring.childContext.remote;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.models.Project;
import org.gitlab4j.api.models.ProjectHook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.reactive.context.ReactiveWebServerApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import org.taruts.dynamicJava.app.OurSmartLifecycle;
import org.taruts.dynamicJava.app.controller.refresh.RefreshController;
import org.taruts.dynamicJava.app.dynamicWiring.DynamicProject;
import org.taruts.dynamicJava.app.dynamicWiring.DynamicProjectRepository;
import org.taruts.dynamicJava.app.dynamicWiring.childContext.configurationProperties.DynamicImplProperties;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Comparator;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * For any dynamic project registers a webhook in the dynamic code GitLab project, after the application has started
 * and removes the hook on shutdown.
 * The webhook notifies the application when a new version of the dynamic code has been pushed.
 *
 * @see RefreshController
 */
@Profile({"dev", "prod"})
@Component
@Slf4j
public class GitlabHookRegistrar extends OurSmartLifecycle implements Ordered {

    private static final Comparator<URL> URL_COMPARATOR = Comparator
            .comparing(URL::getHost)
            .thenComparing(URL::getPort)
            .thenComparing(URL::getPath);

    @Autowired
    private DynamicImplProperties dynamicImplProperties;

    @Autowired
    private ReactiveWebServerApplicationContext reactiveWebServerApplicationContext;

    @Autowired
    private DynamicProjectRepository dynamicProjectRepository;

    @Override
    public int getOrder() {
        return -1;
    }

    @Override
    public void doStop() {
        dynamicProjectRepository.forEachProject(this::deleteDynamicProjectHooks);
    }

    @Override
    public void doStart() {
        dynamicProjectRepository.forEachProject(this::replaceHook);
    }

    private void replaceHook(DynamicProject dynamicProject) {

        URL hookUrl = getHookUrl(dynamicProject);
        if (hookUrl == null) {
            return;
        }
        withGitLabProject((gitLabApi, gitLabProject) -> {
            deleteHooksByUrl(gitLabApi, gitLabProject, hookUrl);
            addHook(gitLabApi, gitLabProject, hookUrl);
        });
    }

    @SneakyThrows
    private URL getHookUrl(DynamicProject dynamicProject) {
        DynamicImplProperties.GitRepository.Hook hookProperties = dynamicImplProperties.getGitRepository().getHook();

        String host = hookProperties.getHost();
        if (StringUtils.isBlank(host)) {
            return null;
        }

        String protocol = hookProperties.getProtocol();

        int ourServletContainerPort = reactiveWebServerApplicationContext.getWebServer().getPort();

        return UriComponentsBuilder
                .newInstance()
                .scheme(protocol)
                .host(host)
                .port(ourServletContainerPort)
                .replacePath("refresh")
                .path(dynamicProject.getName())
                .build()
                .toUri()
                .toURL();
    }

    private void deleteDynamicProjectHooks(DynamicProject dynamicProject) {
        URL hookUri = getHookUrl(dynamicProject);
        if (hookUri == null) {
            return;
        }
        withGitLabProject((gitLabApi, gitLabProject) ->
                deleteHooksByUrl(gitLabApi, gitLabProject, hookUri)
        );
    }

    @SneakyThrows
    private void withGitLabProject(BiConsumer<GitLabApi, Project> useProject) {
        DynamicImplProperties.GitRepository gitRepositoryProperties = dynamicImplProperties.getGitRepository();
        String repositoryUrlStr = gitRepositoryProperties.getUrl();
        String username = gitRepositoryProperties.getUsername();
        String password = gitRepositoryProperties.getPassword();

        URI repositoryUri = URI.create(repositoryUrlStr);

        String repositoryPath = repositoryUri.getPath();
        String projectPath = repositoryUri.getPath()
                .substring(
                        // After the first "/"
                        1,
                        // Up to the trailing ".git"
                        repositoryPath.lastIndexOf(".git")
                );

        UriComponents gitlabUriComponents = UriComponentsBuilder
                .fromUri(repositoryUri)
                .replacePath(null)
                .build();
        String gitlabUrlStr = gitlabUriComponents.toString();

        try (
                GitLabApi gitLabApi = GitLabApi.oauth2Login(gitlabUrlStr, username, password, true)
        ) {
            Project project = gitLabApi
                    .getProjectApi()
                    .getOptionalProject(projectPath)
                    .orElseThrow(() -> new IllegalStateException("Project not found by path %s".formatted(projectPath)));
            useProject.accept(gitLabApi, project);
        }
    }

    private void deleteHooksByUrl(GitLabApi gitLabApi, Project project, URL hookUrl) {
        List<ProjectHook> hooksToDelete = findHooksToDeleteByUrl(gitLabApi, project, hookUrl);
        hooksToDelete.forEach(hookToDelete -> {
            try {
                gitLabApi.getProjectApi().deleteHook(hookToDelete);
            } catch (GitLabApiException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @SneakyThrows
    private List<ProjectHook> findHooksToDeleteByUrl(GitLabApi gitLabApi, Project project, URL hookUrl) {
        List<ProjectHook> allProjectHooks = gitLabApi.getProjectApi().getHooks(project.getId());
        return allProjectHooks.stream().filter(currentHook -> {
            try {
                URL currentHookUrl = new URL(currentHook.getUrl());
                return URL_COMPARATOR.compare(currentHookUrl, hookUrl) == 0;
            } catch (MalformedURLException e) {
                // All bad URLs must be deleted
                return true;
            }
        }).toList();
    }

    private void addHook(GitLabApi gitLabApi, Project gitLabProject, URL hookUrl) {
        DynamicImplProperties.GitRepository.Hook hookProperties = dynamicImplProperties.getGitRepository().getHook();

        boolean enableSslVerification = hookProperties.isSslVerification();
        String secretToken = hookProperties.getSecretToken();

        ProjectHook enabledHooks = new ProjectHook();

        //@formatter:off
        enabledHooks.setPushEvents               (true);
        enabledHooks.setPushEventsBranchFilter   ("master");
        enabledHooks.setIssuesEvents             (false);
        enabledHooks.setConfidentialIssuesEvents (false);
        enabledHooks.setMergeRequestsEvents      (false);
        enabledHooks.setTagPushEvents            (false);
        enabledHooks.setNoteEvents               (false);
        enabledHooks.setConfidentialNoteEvents   (false);
        enabledHooks.setJobEvents                (false);
        enabledHooks.setPipelineEvents           (false);
        enabledHooks.setWikiPageEvents           (false);
        enabledHooks.setRepositoryUpdateEvents   (false);
        enabledHooks.setDeploymentEvents         (false);
        enabledHooks.setReleasesEvents           (false);
        enabledHooks.setDeploymentEvents         (false);
        //@formatter:on

        try {
            gitLabApi.getProjectApi().addHook(gitLabProject.getId(), hookUrl.toString(), enabledHooks, enableSslVerification, secretToken);
        } catch (GitLabApiException e) {
            throw new RuntimeException(e);
        }
    }
}
