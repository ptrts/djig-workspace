package app.main.dynamic.wiring;

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
    }
}
