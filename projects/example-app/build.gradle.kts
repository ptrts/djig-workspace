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

    // The API that the core of our application and the dynamic part use to communicate to each other
    implementation("org.taruts.djig.example:dynamic-api:001")

    // Utils
    implementation("org.apache.commons:commons-lang3")
    implementation("commons-io:commons-io:2.11.0")
    implementation("com.google.guava:guava:31.1-jre")

    implementation("org.taruts.djig:core:001")

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
