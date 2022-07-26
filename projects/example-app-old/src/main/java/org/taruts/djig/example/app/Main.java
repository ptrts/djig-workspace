package org.taruts.djig.example.app;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;
import org.taruts.djig.core.childContext.configurationProperties.DjigConfigurationProperties;

@SpringBootApplication(scanBasePackages = "org.taruts.djig.example.app")
@EnableConfigurationProperties(DjigConfigurationProperties.class)

// todo Эти штуки как-то перенести в библиотеку
@EnableAsync
@Slf4j
public class Main {

    public static void main(String[] args) {
        SpringApplication.run(Main.class, args);
    }
}
