package org.taruts.djig.app;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;
import org.taruts.djig.app.dynamicWiring.childContext.configurationProperties.DynamicProjectsConfigurationProperties;

@SpringBootApplication(scanBasePackages = "org.taruts.dynamicJava.app")
@EnableConfigurationProperties(DynamicProjectsConfigurationProperties.class)
@EnableAsync
@EnableRetry
@Slf4j
public class Main {

    public static void main(String[] args) {
        SpringApplication.run(Main.class, args);
    }
}