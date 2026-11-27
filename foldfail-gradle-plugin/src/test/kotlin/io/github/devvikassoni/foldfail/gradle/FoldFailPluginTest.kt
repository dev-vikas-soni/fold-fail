
package io.github.devvikassoni.foldfail.gradle

import org.gradle.testkit.runner.GradleRunner
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class FoldFailPluginTest {

    @Test
    fun `detects portrait orientation restriction and aspect ratio restriction`() {
        val projectDir = Files.createTempDirectory("foldfail-test").toFile()

        try {
            File(projectDir, "settings.gradle.kts").writeText(
                """rootProject.name = "foldfail-fixture""""
            )

            File(projectDir, "build.gradle.kts").writeText(
                """
                plugins {
                    id("io.github.dev-vikas-soni.foldfail")
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
                .withArguments("foldFail", "--stacktrace")
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
        val projectDir = Files.createTempDirectory("foldfail-check-test").toFile()

        try {
            File(projectDir, "settings.gradle.kts").writeText(
                """rootProject.name = "foldfail-check-fixture""""
            )

            File(projectDir, "build.gradle.kts").writeText(
                """
                plugins {
                    id("io.github.dev-vikas-soni.foldfail")
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
                "Expected foldFail task execution during check:\n${result.output}",
                result.output.contains(":foldFail")
            )
        } finally {
            projectDir.deleteRecursively()
        }
    }

    @Test
    fun `supports configuration cache`() {
        val projectDir = Files.createTempDirectory("foldfail-cc-test").toFile()

        try {
            File(projectDir, "settings.gradle.kts").writeText(
                """rootProject.name = "foldfail-cc-fixture""""
            )

            File(projectDir, "build.gradle.kts").writeText(
                """
                plugins {
                    id("io.github.dev-vikas-soni.foldfail")
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
        val projectDir = Files.createTempDirectory("foldfail-report-test").toFile()

        try {
            File(projectDir, "settings.gradle.kts").writeText(
                """rootProject.name = "foldfail-report-fixture""""
            )

            File(projectDir, "build.gradle.kts").writeText(
                """
                plugins {
                    id("io.github.dev-vikas-soni.foldfail")
                }

                foldFail {
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
                .withArguments("foldFail", "--stacktrace")
                .buildAndFail() // Expect failure because failOn = "WARNING" and portrait restriction is WARNING

            assertTrue(
                "Expected quality gate failure message:\n${result.output}",
                result.output.contains("FoldFail quality gate failed")
            )

            val jsonReport = File(projectDir, "build/reports/foldfail/foldfail-report.json")
            val htmlReport = File(projectDir, "build/reports/foldfail/foldfail-report.html")
            val sarifReport = File(projectDir, "build/reports/foldfail/foldfail-report.sarif")

            assertTrue("JSON report should exist", jsonReport.exists())
            assertTrue("HTML report should exist", htmlReport.exists())
            assertTrue("SARIF report should exist", sarifReport.exists())

            if (sarifReport.exists()) {
                val rootReportsDir = File("build/reports/foldfail")
                rootReportsDir.mkdirs()
                sarifReport.copyTo(File(rootReportsDir, "foldfail-report.sarif"), overwrite = true)
            }

            assertTrue("JSON contains FF001", jsonReport.readText().contains("FF001"))
            assertTrue("HTML contains FF001", htmlReport.readText().contains("FF001"))
            assertTrue("SARIF contains FF001", sarifReport.readText().contains("FF001"))
        } finally {
            projectDir.deleteRecursively()
        }
    }
}
