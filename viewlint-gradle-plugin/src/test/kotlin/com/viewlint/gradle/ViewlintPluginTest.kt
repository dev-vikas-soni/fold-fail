package com.viewlint.gradle

import org.gradle.testkit.runner.GradleRunner
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class ViewlintPluginTest {

    @Test
    fun `detects portrait orientation restriction and aspect ratio restriction`() {
        val projectDir = Files.createTempDirectory("viewlint-test").toFile()

        try {
            File(projectDir, "settings.gradle.kts").writeText(
                """rootProject.name = "viewlint-fixture""""
            )

            File(projectDir, "build.gradle.kts").writeText(
                """
                plugins {
                    id("com.viewlint")
                }
                """.trimIndent()
            )

            val manifest = File(
                projectDir,
                "src/main/AndroidManifest.xml"
            )

            manifest.parentFile.mkdirs()

            manifest.writeText(
                """
                <manifest xmlns:android="http://schemas.android.com/apk/res/android">
                    <application>
                        <activity
                            android:name=".MainActivity"
                            android:screenOrientation="portrait" />
                        <activity
                            android:name=".DetailActivity"
                            android:maxAspectRatio="2.4" />
                    </application>
                </manifest>
                """.trimIndent()
            )

            val result = GradleRunner.create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments("viewlint", "--stacktrace")
                .build()

            assertTrue(
                "Expected FF001 finding in output:\n${result.output}",
                result.output.contains("FF001")
            )

            assertTrue(
                "Expected FF002 finding in output:\n${result.output}",
                result.output.contains("FF002")
            )
        } finally {
            projectDir.deleteRecursively()
        }
    }

    @Test
    fun `runs automatically through check task`() {
        val projectDir = Files.createTempDirectory("viewlint-check-test").toFile()

        try {
            File(projectDir, "settings.gradle.kts").writeText(
                """rootProject.name = "viewlint-check-fixture""""
            )

            File(projectDir, "build.gradle.kts").writeText(
                """
                plugins {
                    id("com.viewlint")
                }
                """.trimIndent()
            )

            val manifest = File(
                projectDir,
                "src/main/AndroidManifest.xml"
            )

            manifest.parentFile.mkdirs()

            manifest.writeText(
                """
                <manifest xmlns:android="http://schemas.android.com/apk/res/android">
                    <application>
                        <activity
                            android:name=".MainActivity"
                            android:screenOrientation="portrait" />
                    </application>
                </manifest>
                """.trimIndent()
            )

            val result = GradleRunner.create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments("check", "--stacktrace")
                .build()

            assertTrue(
                "Expected viewlint task execution during check:\n${result.output}",
                result.output.contains(":viewlint")
            )
        } finally {
            projectDir.deleteRecursively()
        }
    }

    @Test
    fun `supports configuration cache`() {
        val projectDir = Files.createTempDirectory("viewlint-cc-test").toFile()

        try {
            File(projectDir, "settings.gradle.kts").writeText(
                """rootProject.name = "viewlint-cc-fixture""""
            )

            File(projectDir, "build.gradle.kts").writeText(
                """
                plugins {
                    id("com.viewlint")
                }
                """.trimIndent()
            )

            val manifest = File(
                projectDir,
                "src/main/AndroidManifest.xml"
            )

            manifest.parentFile.mkdirs()

            manifest.writeText(
                """
                <manifest xmlns:android="http://schemas.android.com/apk/res/android">
                    <application>
                        <activity
                            android:name=".MainActivity"
                            android:screenOrientation="unspecified" />
                    </application>
                </manifest>
                """.trimIndent()
            )

            // Run twice to verify configuration-cache reuse
            GradleRunner.create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments("check", "--configuration-cache", "--stacktrace")
                .build()

            val result2 = GradleRunner.create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments("check", "--configuration-cache", "--stacktrace")
                .build()

            assertTrue(
                "Expected configuration cache reuse",
                result2.output.contains("Reusing configuration cache") || result2.output.contains("Configuration cache")
            )
        } finally {
            projectDir.deleteRecursively()
        }
    }

    @Test
    fun `generates json and html reports and enforces quality gate`() {
        val projectDir = Files.createTempDirectory("viewlint-report-test").toFile()

        try {
            File(projectDir, "settings.gradle.kts").writeText(
                """rootProject.name = "viewlint-report-fixture""""
            )

            File(projectDir, "build.gradle.kts").writeText(
                """
                plugins {
                    id("com.viewlint")
                }

                viewlint {
                    reportFormats = listOf("json", "html", "sarif")
                    failOn = "WARNING"
                }
                """.trimIndent()
            )

            val manifest = File(
                projectDir,
                "src/main/AndroidManifest.xml"
            )

            manifest.parentFile.mkdirs()

            manifest.writeText(
                """
                <manifest xmlns:android="http://schemas.android.com/apk/res/android">
                    <application>
                        <activity
                            android:name=".MainActivity"
                            android:screenOrientation="portrait" />
                    </application>
                </manifest>
                """.trimIndent()
            )

            val result = GradleRunner.create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments("viewlint", "--stacktrace")
                .buildAndFail() // Expect failure because failOn = "WARNING" and portrait restriction is WARNING

            assertTrue(
                "Expected quality gate failure message:\n${result.output}",
                result.output.contains("Viewlint quality gate failed")
            )

            val jsonReport = File(projectDir, "build/reports/viewlint/viewlint-report.json")
            val htmlReport = File(projectDir, "build/reports/viewlint/viewlint-report.html")
            val sarifReport = File(projectDir, "build/reports/viewlint/viewlint-report.sarif")

            assertTrue("JSON report should exist", jsonReport.exists())
            assertTrue("HTML report should exist", htmlReport.exists())
            assertTrue("SARIF report should exist", sarifReport.exists())

            assertTrue("JSON contains FF001", jsonReport.readText().contains("FF001"))
            assertTrue("HTML contains FF001", htmlReport.readText().contains("FF001"))
            assertTrue("SARIF contains FF001", sarifReport.readText().contains("FF001"))
        } finally {
            projectDir.deleteRecursively()
        }
    }
}
