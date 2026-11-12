plugins {
    kotlin("jvm") version "2.0.21" apply false
    kotlin("android") version "2.0.21" apply false
    id("com.android.application") version "8.7.3" apply false
    id("com.android.library") version "8.7.3" apply false
}

extra["gdxVersion"] = "1.13.1"
extra["kotlinVersion"] = "2.0.21"
extra["coroutinesVersion"] = "1.10.1"
extra["serializationVersion"] = "1.8.0"
extra["junitVersion"] = "5.11.4"

tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}
