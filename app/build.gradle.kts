import app.InitAdjacentGitRepoTask
import app.InitDynamicLocalTask

buildscript {
    repositories {
        mavenLocal()
        mavenCentral()
        maven {
            name = "s3MavenRepo"
            url = uri("s3://maven.taruts.net")
            // AwsCredentials without a configuration closure means that there are credentials in a gradle.properties file.
            // Our credentials are in the gradle.properties in the project itself.
            credentials(AwsCredentials::class)
        }
    }
    dependencies {
        classpath("org.apache.commons:commons-lang3:3.12.0")
        classpath("commons-io:commons-io:2.11.0")
        classpath("com.google.guava:guava:31.1-jre")
        classpath("we:process-utils:001")
        classpath("we:git-utils:001")
        classpath("javax.inject:javax.inject:1")
        classpath("org.gitlab4j:gitlab4j-api:5.0.1")
    }
}

plugins {
    id("org.springframework.boot") version "2.7.0"
    id("io.spring.dependency-management") version "1.0.11.RELEASE"
    id("application")
}

group = "org.taruts.dynamic-java-code-stored-in-git"

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
    implementation("we.git-implementations:dynamic-api:001")

    // GitLab API
    implementation("org.gitlab4j:gitlab4j-api:5.0.1")

    // Our util libraries
    implementation("we:git-utils:001")
    implementation("we:gradle-utils:001")

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

val initDynamicLocal = tasks.registering(InitDynamicLocalTask::class) {
    mustRunAfter(":gitlab-container:createAll")
}

val initLocalGitLab by tasks.registering {
    group = "app"

    description = """
    An aggregator task that creates a local GitLab Docker container, 
    creates a project in there which is a fork of dynamic-dev 
    and clones it into the dynamic-local project subdirectory
    """.trimIndent()

    dependsOn(":gitlab-container:createAll", initDynamicLocal)
}

tasks.register("initDynamicApi", InitAdjacentGitRepoTask::class, "dynamic-api").configure {
    mustRunAfter(initLocalGitLab)
}

tasks.register("initDynamicDev", InitAdjacentGitRepoTask::class, "dynamic-dev").configure {
    mustRunAfter(initLocalGitLab)
}

tasks.register("initProject") {
    group = "app"

    description = """
    Initializes everything for the project. 
    This is an aggregator task for initLocalGitLab, initDynamicApi and initDynamicDev
    """.trimIndent()

    dependsOn(initLocalGitLab, "initDynamicApi", "initDynamicDev")
}
