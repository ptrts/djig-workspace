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
}
