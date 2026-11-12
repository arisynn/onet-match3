plugins {
    `java-library`
    kotlin("jvm")
}

val gdxVersion = rootProject.extra["gdxVersion"] as String
val coroutinesVersion = rootProject.extra["coroutinesVersion"] as String
val serializationVersion = rootProject.extra["serializationVersion"] as String
val junitVersion = rootProject.extra["junitVersion"] as String

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    // libGDX core
    api("com.badlogicgames.gdx:gdx:$gdxVersion")

    // Kotlin Coroutines & Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$serializationVersion")

    // Testing
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:$junitVersion")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    incremental = false
}
