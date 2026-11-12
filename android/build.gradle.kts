plugins {
    id("com.android.application")
    kotlin("android")
}

val gdxVersion = rootProject.extra["gdxVersion"] as String
val natives by configurations.creating

android {
    namespace = "com.onetmatch3.game"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.onetmatch3.game"
        minSdk = 24
        targetSdk = 35
        versionCode = 4
        versionName = "1.0.3"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    sourceSets {
        getByName("main") {
            assets.srcDirs(files("../assets"))
            jniLibs.srcDirs(layout.buildDirectory.dir("generated/jniLibs"))
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    signingConfigs {
        create("release") {
            val keystoreFile = System.getenv("ANDROID_KEYSTORE_FILE")
            if (!keystoreFile.isNullOrBlank() && file(keystoreFile).exists()) {
                storeFile = file(keystoreFile)
                storePassword = System.getenv("ANDROID_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("ANDROID_KEY_ALIAS")
                keyPassword = System.getenv("ANDROID_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            val keystoreFile = System.getenv("ANDROID_KEYSTORE_FILE")
            if (!keystoreFile.isNullOrBlank() && file(keystoreFile).exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
        debug {
            isDebuggable = true
        }
    }
}

dependencies {
    implementation(project(":core"))
    implementation("com.badlogicgames.gdx:gdx:$gdxVersion")
    implementation("com.badlogicgames.gdx:gdx-backend-android:$gdxVersion")

    natives("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-armeabi-v7a")
    natives("com.badlogicgames.gdx:gdx-freetype-platform:$gdxVersion:natives-armeabi-v7a")
    natives("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-arm64-v8a")
    natives("com.badlogicgames.gdx:gdx-freetype-platform:$gdxVersion:natives-arm64-v8a")
    natives("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-x86")
    natives("com.badlogicgames.gdx:gdx-freetype-platform:$gdxVersion:natives-x86")
    natives("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-x86_64")
    natives("com.badlogicgames.gdx:gdx-freetype-platform:$gdxVersion:natives-x86_64")

    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:core:1.6.1")

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
}

tasks.register("copyAndroidNatives") {
    doFirst {
        layout.buildDirectory.dir("generated/jniLibs/armeabi-v7a").get().asFile.mkdirs()
        layout.buildDirectory.dir("generated/jniLibs/arm64-v8a").get().asFile.mkdirs()
        layout.buildDirectory.dir("generated/jniLibs/x86").get().asFile.mkdirs()
        layout.buildDirectory.dir("generated/jniLibs/x86_64").get().asFile.mkdirs()

        natives.files.forEach { jar ->
            var outputDir: File? = null
            if (jar.name.endsWith("natives-arm64-v8a.jar")) outputDir = layout.buildDirectory.dir("generated/jniLibs/arm64-v8a").get().asFile
            if (jar.name.endsWith("natives-armeabi-v7a.jar")) outputDir = layout.buildDirectory.dir("generated/jniLibs/armeabi-v7a").get().asFile
            if (jar.name.endsWith("natives-x86_64.jar")) outputDir = layout.buildDirectory.dir("generated/jniLibs/x86_64").get().asFile
            if (jar.name.endsWith("natives-x86.jar")) outputDir = layout.buildDirectory.dir("generated/jniLibs/x86").get().asFile
            outputDir?.let { dest ->
                copy {
                    from(zipTree(jar))
                    into(dest)
                    include("*.so")
                }
            }
        }
    }
}

tasks.matching { it.name.contains("merge") && it.name.contains("JniLibFolders") }.configureEach {
    dependsOn("copyAndroidNatives")
}

tasks.named("preBuild").configure {
    dependsOn("copyAndroidNatives")
}
