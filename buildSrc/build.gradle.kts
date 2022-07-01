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
    implementation("org.taruts:git-utils:001")
    implementation("org.taruts:gradle-utils:001")
    implementation("org.taruts:process-utils:001")

    // Utils
    implementation("org.apache.commons:commons-lang3:3.12.0")
    implementation("commons-io:commons-io:2.11.0")
    implementation("com.google.guava:guava:31.1-jre")

    // Other libraries
    implementation("javax.inject:javax.inject:1")
    implementation("org.slf4j:slf4j-simple:1.7.36")
    implementation("org.gitlab4j:gitlab4j-api:5.0.1")
    implementation("io.projectreactor.netty:reactor-netty-http:1.0.18")
    implementation("io.netty:netty-transport-native-unix-common:4.1.74.Final")
}
