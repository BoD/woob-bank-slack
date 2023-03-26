import com.bmuschko.gradle.docker.tasks.image.DockerBuildImage
import com.bmuschko.gradle.docker.tasks.image.Dockerfile
import com.bmuschko.gradle.docker.tasks.image.Dockerfile.FromInstruction

plugins {
    kotlin("jvm")
    kotlin("kapt")
    application
    id("com.bmuschko.docker-java-application")
}

group = "org.jraf"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation(KotlinX.coroutines.jdk8)

    // Retrofit / Moshi
    implementation(Square.retrofit2)
    implementation(Square.retrofit2.converter.moshi)
    implementation(Square.moshi)
    kapt(Square.moshi.kotlinCodegen)

    implementation(KotlinX.cli)
}

application {
    mainClass.set("MainKt")
}

docker {
    javaApplication {
        maintainer.set("BoD <BoD@JRAF.org>")
        ports.set(emptyList())
        images.add("bodlulu/${rootProject.name}:latest")
        jvmArgs.set(listOf("-Xms16m", "-Xmx128m"))
    }
    registryCredentials {
        username.set(System.getenv("DOCKER_USERNAME"))
        password.set(System.getenv("DOCKER_PASSWORD"))
    }

}

tasks.withType<DockerBuildImage> {
    platform.set("linux/amd64")
}

tasks.withType<Dockerfile> {
    // Use OpenJ9 instead of the default one
    instructions.set(
        instructions.get().map { item ->
            if (item.keyword == FromInstruction.KEYWORD) {
                FromInstruction(Dockerfile.From("adoptopenjdk/openjdk11-openj9:x86_64-ubuntu-jre-11.0.18_10_openj9-0.36.1"))
            } else {
                item
            }
        }
    )

    // Install python
    runCommand(
        """
            apt-get update && \
            apt-get install -y software-properties-common && \
            apt-get install -y python3-pip
        """.trimIndent()
    )
    // Install woob from source
    runCommand("apt-get install -y git")
    runCommand("git clone https://gitlab.com/woob/woob.git --depth 1")
    runCommand("cd woob && pip install .")
    runCommand("woob update")
    environmentVariable("MALLOC_ARENA_MAX", "4")
}


// `./gradlew refreshVersions` to update dependencies
// `DOCKER_USERNAME=<your docker hub login> DOCKER_PASSWORD=<your docker hub password> ./gradlew dockerPushImage` to build and push the image
