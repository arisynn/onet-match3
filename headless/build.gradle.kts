plugins {
    kotlin("jvm")
}

val gdxVersion = rootProject.extra["gdxVersion"] as String
val junitVersion = rootProject.extra["junitVersion"] as String

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(project(":core"))
    implementation("com.badlogicgames.gdx:gdx-backend-headless:$gdxVersion")
    testImplementation("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-desktop")

    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:$junitVersion")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
