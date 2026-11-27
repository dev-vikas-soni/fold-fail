pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        google()
    }

    plugins {
        id("org.jetbrains.kotlin.jvm") version "2.3.20"
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        google()
    }
}

rootProject.name = "fold-fail"

include(
    ":foldfail-core",
    ":foldfail-analyzer",
    ":foldfail-gradle-plugin"
)
