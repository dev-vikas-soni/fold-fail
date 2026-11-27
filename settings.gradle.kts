pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        google()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        google()
    }
}

rootProject.name = "viewlint"

include(
    ":viewlint-core",
    ":viewlint-analyzer",
    ":viewlint-gradle-plugin"
)
