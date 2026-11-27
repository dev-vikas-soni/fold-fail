plugins {
    id("org.jetbrains.kotlin.jvm")
}

dependencies {
    implementation(project(":viewlint-core"))
    implementation("org.jetbrains.kotlin:kotlin-compiler-embeddable:2.3.20")

    testImplementation("junit:junit:4.13.2")
}

tasks.test {
    useJUnit()
}
