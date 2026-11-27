plugins {
    id("org.jetbrains.kotlin.jvm") version "2.3.20" apply false
}

allprojects {
    group = "io.github.dev-vikas-soni"
    version = "0.7.1"

    plugins.withId("org.jetbrains.kotlin.jvm") {
        tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
            compilerOptions {
                jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
            }
        }
        tasks.withType<JavaCompile>().configureEach {
            options.release.set(21)
        }
    }
}
