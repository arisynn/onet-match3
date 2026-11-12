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
