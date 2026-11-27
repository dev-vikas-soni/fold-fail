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

rootProject.name = "fold-fail"

include(
    ":foldfail-core",
    ":foldfail-analyzer",
    ":foldfail-gradle-plugin"
)
