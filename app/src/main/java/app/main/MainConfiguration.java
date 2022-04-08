package app.main;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.nio.file.Path;

@Configuration
public class MainConfiguration {

    @Bean("dynamicImplSourceDirectory")
    File dynamicImplSourceDirectory() {
        String directoryName = "dynamic";
        Path path = Path.of(directoryName).toAbsolutePath().normalize();
        return path.toFile();
    }
}
