import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import us.kirchmeier.capsule.spec.ReallyExecutableSpec
import us.kirchmeier.capsule.task.FatCapsule

plugins {
    kotlin("jvm") version Versions.KOTLIN
    kotlin("kapt") version Versions.KOTLIN
    application
    id("it.gianluz.capsule") version Versions.CAPSULE_PLUGIN
}

group = "org.jraf"
version = "1.0.0"

repositories {
    mavenCentral()
    maven { url = uri("https://kotlin.bintray.com/kotlinx") }
}

dependencies {
    // Note: using compile instead of implementation because for some unknown reason, implementation
    // doesn't play well when generating the fatCapsule.
    compile(kotlin("stdlib-jdk8", Versions.KOTLIN))
    implementation("org.jetbrains.kotlinx", "kotlinx-coroutines-jdk8", Versions.COROUTINES)

    // Retrofit / Moshi
    implementation("com.squareup.retrofit2", "retrofit", Versions.RETROFIT)
    implementation("com.squareup.retrofit2", "converter-moshi", Versions.RETROFIT)
    implementation("com.squareup.moshi", "moshi", Versions.MOSHI)
    kapt("com.squareup.moshi", "moshi-kotlin-codegen", Versions.MOSHI)

    compile("org.jetbrains.kotlinx", "kotlinx-cli", Versions.KOTLINX_CLI)

    testImplementation(kotlin("test-junit"))
}

tasks.test {
    useJUnit()
}

tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = "1.8"
}

tasks.create<FatCapsule>("fatCapsule") {
    applicationClass("MainKt")
    reallyExecutable(closureOf<ReallyExecutableSpec> { regular() })
}

application {
    mainClassName = "MainKt"
}

// run "./gradlew fatCapsule" to build the "really executable fat jar"