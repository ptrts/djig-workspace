package org.taruts.dynamicJava.app.dynamicWiring.childContext.configurationProperties;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Getter
public class DynamicImplProperties {

    @Autowired
    GitRepository gitRepository;

    @Component
    @Getter
    public static class GitRepository {

        @Value("${dynamic-impl.git-repository.url}")
        String url;

        @Value("${dynamic-impl.git-repository.username:}")
        String username;

        @Value("${dynamic-impl.git-repository.password:}")
        String password;

        @Autowired
        Hook hook;

        @Component
        @Getter
        public static class Hook {
            @Value("${dynamic-impl.git-repository.hook.protocol:}")
            String protocol;

            @Value("${dynamic-impl.git-repository.hook.host:}")
            String host;

            @Value("${dynamic-impl.git-repository.hook.ssl-verification:false}")
            boolean sslVerification;

            @Value("${dynamic-impl.git-repository.hook.secret-token:}")
            String secretToken;
        }
    }
}
