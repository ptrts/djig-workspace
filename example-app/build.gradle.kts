import djig.DjigPlugin
import djig.DjigPluginExtension
import gitlabContainer.GitLabContainerPlugin
import gitlabContainer.GitLabContainerPluginExtension
import java.net.URL

plugins {
    id("org.springframework.boot") version "2.7.0"
    id("io.spring.dependency-management") version "1.0.11.RELEASE"
    id("application")
}

group = "org.taruts.djig.example"

java.sourceCompatibility = JavaVersion.VERSION_17

configurations {
    compileOnly {
        extendsFrom(configurations["annotationProcessor"])
    }
}

repositories {
    mavenLocal()
    mavenCentral()
    maven {
        name = "s3MavenRepo"
        url = uri("s3://maven.taruts.net")
        // AwsCredentials without a configuration closure means that there are credentials in a gradle.properties file.
        // Our credentials are in the gradle.properties in the project.
        credentials(AwsCredentials::class)
    }
}

dependencies {
    // Dev tools
    developmentOnly("org.springframework.boot:spring-boot-devtools")

    // Lombok
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    // Spring Boot + WebFlux
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    // Spring Retry
    implementation("org.springframework:spring-aspects")
    implementation("org.springframework.retry:spring-retry:1.2.5.RELEASE")

    // The API that the core of our application and the dynamic part use to communicate to each other
    implementation("org.taruts.djig.example:dynamic-api:001")

    // GitLab API
    implementation("org.gitlab4j:gitlab4j-api:5.0.1")

    // Our util libraries
    implementation("org.taruts:git-utils:001")
    implementation("org.taruts:gradle-utils:001")

    // Utils
    implementation("org.apache.commons:commons-lang3")
    implementation("commons-io:commons-io:2.11.0")
    implementation("com.google.guava:guava:31.1-jre")

    // Reflections
    implementation("org.reflections:reflections:0.10.2")

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Tests
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    // Spring Boot
    testImplementation("org.springframework.boot:spring-boot-starter-test")

    // Project Reactor
    testImplementation("io.projectreactor:reactor-test")
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}

apply<GitLabContainerPlugin>()
configure<GitLabContainerPluginExtension> {
    url.set(URL("http://localhost:9580"))
    username.set("user")
    password.set("123456789")
}

apply<DjigPlugin>()
configure<DjigPluginExtension> {
    localGitLabsCreation {
        sourceSpringBootProfile.set("dynamic-dev")
        targetGitLabs.fromGitLabContainer("dynamic-local", "dynamic-local-")
    }
}
