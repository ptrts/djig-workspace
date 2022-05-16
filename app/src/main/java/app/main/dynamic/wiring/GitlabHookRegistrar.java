package app.main.dynamic.wiring;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.models.Project;
import org.gitlab4j.api.models.ProjectHook;
import org.gitlab4j.api.models.Setting;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;

@Profile({"dev", "prod"})
@Component
@Slf4j
public class GitlabHookRegistrar implements SmartLifecycle {

    private boolean running = false;

    @Autowired
    private DynamicImplProperties dynamicImplProperties;

    @Override
    @SneakyThrows
    public void start() {

        withProject((gitLabApi, project) -> {
            try {
                gitLabApi.getApplicationSettingsApi().updateApplicationSetting(Setting.ALLOW_LOCAL_REQUESTS_FROM_SYSTEM_HOOKS, true);
                gitLabApi.getApplicationSettingsApi().updateApplicationSetting(Setting.ALLOW_LOCAL_REQUESTS_FROM_WEB_HOOKS_AND_SERVICES, true);
            } catch (GitLabApiException e) {
                throw new RuntimeException(e);
            }

            URI hookUri = getHookUri();

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

        withProject((gitLabApi, project) -> {
            URI hookUri = getHookUri();
            deleteHooks(gitLabApi, project, hookUri);
        });

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
        return UriComponentsBuilder
                .fromHttpUrl(dynamicImplProperties.getGitRepository().getHook().getBaseUrl())
                .replacePath("refresh")
                .build()
                .toUri();
    }
}
