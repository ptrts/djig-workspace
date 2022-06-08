plugins {
    id("org.jetbrains.kotlin.jvm") version "1.6.21"
}

repositories {
    mavenLocal()
    mavenCentral()
    maven {
        name = "s3MavenRepo"
        url = uri("s3://maven.taruts.net")
        credentials(AwsCredentials::class)
    }
}

dependencies {

    // Our libraries
    implementation("we:git-utils:001")
    implementation("we:gradle-utils:001")
    implementation("we:process-utils:001")

    // Utils
    implementation("org.apache.commons:commons-lang3:3.12.0")
    implementation("commons-io:commons-io:2.11.0")
    implementation("com.google.guava:guava:31.1-jre")

    // Other libraries
    implementation("javax.inject:javax.inject:1")
    implementation("org.gitlab4j:gitlab4j-api:5.0.1")
}
