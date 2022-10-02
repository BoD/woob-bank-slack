import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    kotlin("jvm")
    kotlin("kapt")
    application
    id("com.github.johnrengelman.shadow")
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

tasks {
    named<ShadowJar>("shadowJar") {
        minimize()
    }
}

// Implements https://github.com/brianm/really-executable-jars-maven-plugin maven plugin behaviour.
// To check details how it works, see http://skife.org/java/unix/2011/06/20/really_executable_jars.html.
tasks.register<DefaultTask>("shadowJarExecutable") {
    description = "Creates a self-executable file, that runs the generated shadow jar"
    group = "Distribution"

    inputs.files(tasks.named("shadowJar"))
    val origFile = inputs.files.singleFile
    outputs.files(File(origFile.parentFile, origFile.nameWithoutExtension + "-executable"))

    doLast {
        val execFile: File = outputs.files.files.first()
        val out = execFile.outputStream()
        out.write("#!/bin/sh\n\nexec java -jar \"\$0\" \"\$@\"\n\n".toByteArray())
        out.write(inputs.files.singleFile.readBytes())
        out.flush()
        out.close()
        execFile.setExecutable(true, false)
    }
}

// `./gradlew refreshVersions` to update dependencies
// `./gradlew shadowJarExecutable` to build the "really executable jar"
