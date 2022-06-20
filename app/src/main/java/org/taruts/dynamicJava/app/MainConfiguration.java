package org.taruts.dynamicJava.app;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.nio.file.Path;

@Configuration
public class MainConfiguration {

    /**
     * @return {@link File} representing the subdirectory "dynamic" of the working directory.
     * This is where the application will clone the dynamic code into and build the code.
     */
    @Bean("dynamicImplSourceDirectory")
    File dynamicImplSourceDirectory() {
        String directoryName = "dynamic";
        Path path = Path.of(directoryName).toAbsolutePath().normalize();
        return path.toFile();
    }
}
