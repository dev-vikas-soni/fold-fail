plugins {
    id("org.jetbrains.kotlin.jvm")
    `java-gradle-plugin`
}


dependencies {
    implementation(project(":foldfail-core"))
    implementation(project(":foldfail-analyzer"))

    testImplementation("junit:junit:4.13.2")
    testImplementation(gradleTestKit())
}

tasks.test {
    useJUnit()
}

gradlePlugin {
    plugins {
        create("foldFail") {
            id = "io.github.dev-vikas-soni.foldfail"
            implementationClass = "io.github.devvikassoni.foldfail.gradle.FoldFailPlugin"
            displayName = "FoldFail"
            description = "Adaptive Android quality auditor"
        }
    }
}
