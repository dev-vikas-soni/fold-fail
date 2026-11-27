plugins {
    id("org.jetbrains.kotlin.jvm")
    `java-gradle-plugin`
    id("com.gradleup.shadow") version "9.6.1"
    id("com.gradle.plugin-publish") version "2.2.1"
}

dependencies {
    implementation(project(":viewlint-core"))
    implementation(project(":viewlint-analyzer"))
    implementation("org.jetbrains.kotlin:kotlin-compiler-embeddable:2.3.20")

    testImplementation("junit:junit:4.13.2")
    testImplementation(gradleTestKit())
}

tasks.test {
    useJUnit()
}

tasks.named("shadowJar", org.gradle.api.tasks.bundling.Jar::class.java) {
    archiveClassifier.set("")
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}

tasks.jar {
    enabled = false
    dependsOn(tasks.named("shadowJar"))
}

gradlePlugin {
    website.set("https://github.com/dev-vikas-soni/viewlint")
    vcsUrl.set("https://github.com/dev-vikas-soni/viewlint")
    plugins {
        create("viewlint") {
            id = "com.viewlint"
            implementationClass = "com.viewlint.gradle.ViewlintPlugin"
            displayName = "Viewlint — Android Adaptive Quality Auditor"
            description = "Static analysis for Android adaptive UI risks across phones, foldables, tablets, split-screen and resizable windows. Detects manifest, XML layout and Jetpack Compose issues, with configurable quality gates and SARIF reporting."
            tags.set(listOf("android", "kotlin", "compose", "foldable", "adaptive", "static-analysis", "sarif", "tablet", "multi-window"))
        }
    }
}
