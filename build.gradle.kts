import com.bmuschko.gradle.docker.tasks.image.DockerBuildImage
import com.bmuschko.gradle.docker.tasks.image.Dockerfile
import com.bmuschko.gradle.docker.tasks.image.Dockerfile.CopyFileInstruction

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    application
    id("com.bmuschko.docker-java-application")
}

group = "org.jraf"
version = "1.0.0"

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation("org.slf4j", "slf4j-simple", "_")

    implementation(KotlinX.coroutines.jdk9)

    // Serialization
    implementation(KotlinX.serialization.json)

    // Slack
    implementation("org.jraf.klibslack", "klibslack", "_")

    implementation(KotlinX.cli)
}

application {
    mainClass.set("MainKt")
}

docker {
    javaApplication {
        // Use OpenJ9 instead of the default one
        baseImage.set("adoptopenjdk/openjdk11-openj9:x86_64-ubuntu-jre-11.0.18_10_openj9-0.36.1")
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

    // Move the COPY instructions to the end
    // See https://github.com/bmuschko/gradle-docker-plugin/issues/1093
    instructions.set(
        instructions.get().sortedBy { instruction ->
            if (instruction.keyword == CopyFileInstruction.KEYWORD) 1 else 0
        }
    )
}

// `./gradlew refreshVersions` to update dependencies
// `DOCKER_USERNAME=<your docker hub login> DOCKER_PASSWORD=<your docker hub password> ./gradlew dockerPushImage` to build and push the image
