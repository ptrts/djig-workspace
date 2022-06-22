package org.taruts.dynamicJava.app.dynamicWiring;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.models.Project;
import org.gitlab4j.api.models.ProjectHook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.reactive.context.ReactiveWebServerApplicationContext;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import org.taruts.dynamicJava.app.RefreshController;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;

/**
 * Registers a webhook in the dynamic code GitLab project when the application has started and removes the hook on shutdown.
 * The webhook notifies the application when a new version of the dynamic code has been pushed.
 *
 * @see RefreshController
 */
@Profile({"dev", "prod"})
@Component
@Slf4j
public class GitlabHookRegistrar implements SmartLifecycle {

    private boolean running = false;

    @Autowired
    private DynamicImplProperties dynamicImplProperties;

    @Autowired
    private ReactiveWebServerApplicationContext reactiveWebServerApplicationContext;

    @Override
    @SneakyThrows
    public void start() {

        URI hookUri = getHookUri();
        if (hookUri == null) {
            return;
        }

        withProject((gitLabApi, project) -> {

            DynamicImplProperties.GitRepository gitRepositoryProperties = dynamicImplProperties.getGitRepository();

            boolean enableSslVerification = gitRepositoryProperties.getHook().isSslVerification();
            String secretToken = gitRepositoryProperties.getHook().getSecretToken();

            deleteHooks(gitLabApi, project, hookUri);

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
                gitLabApi.getProjectApi().addHook(project.getId(), hookUri.toString(), enabledHooks, enableSslVerification, secretToken);
            } catch (GitLabApiException e) {
                throw new RuntimeException(e);
            }
        });

        running = true;
    }

    @Override
    public void stop() {

        URI hookUri = getHookUri();
        if (hookUri == null) {
            return;
        }

        withProject((gitLabApi, project) ->
                deleteHooks(gitLabApi, project, hookUri)
        );

        running = false;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @SneakyThrows
    private void withProject(BiConsumer<GitLabApi, Project> useProject) {
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
                        // Until the trailing ".git"
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

    @SneakyThrows
    private void deleteHooks(GitLabApi gitLabApi, Project project, URI hookUri) {
        List<ProjectHook> hooks = gitLabApi.getProjectApi().getHooks(project.getId());
        hooks.stream().filter(currentHook -> {
            try {
                URI currentHookUri = new URI(currentHook.getUrl());
                return Objects.equals(currentHookUri.getHost(), hookUri.getHost());
            } catch (URISyntaxException e) {
                return true;
            }
        }).forEach(currentHook -> {
            try {
                gitLabApi.getProjectApi().deleteHook(currentHook);
            } catch (GitLabApiException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private URI getHookUri() {
        String host = dynamicImplProperties.getGitRepository().getHook().getHost();
        if (StringUtils.isBlank(host)) {
            return null;
        }
        String protocol = dynamicImplProperties.getGitRepository().getHook().getProtocol();
        return UriComponentsBuilder
                .newInstance()
                .scheme(protocol)
                .host(host)
                .port(reactiveWebServerApplicationContext.getWebServer().getPort())
                .replacePath("refresh")
                .build()
                .toUri();
    }
}
