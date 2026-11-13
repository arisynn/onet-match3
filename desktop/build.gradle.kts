plugins {
    kotlin("jvm")
    application
}

val gdxVersion = rootProject.extra["gdxVersion"] as String

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

kotlin {
    jvmToolchain(21)
}

application {
    mainClass.set("game.desktop.DesktopLauncherKt")
}

dependencies {
    implementation(project(":core"))
    implementation("com.badlogicgames.gdx:gdx-backend-lwjgl3:$gdxVersion")
    implementation("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-desktop")
    implementation("com.badlogicgames.gdx:gdx-freetype-platform:$gdxVersion:natives-desktop")
}

sourceSets {
    main {
        resources {
            srcDirs(listOf("../assets"))
        }
    }
}

tasks.named<JavaExec>("run") {
    workingDir = rootDir
}

tasks.register<JavaExec>("previewUi") {
    group = "verification"
    description = "Capture all native UI screens using isolated fixture data."
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("game.desktop.UiPreviewLauncherKt")
    workingDir = rootDir
    args(providers.gradleProperty("previewOutput").orElse("build/ui-preview").get(),
        providers.gradleProperty("previewWidth").orElse("540").get(),
        providers.gradleProperty("previewHeight").orElse("1200").get())
}
