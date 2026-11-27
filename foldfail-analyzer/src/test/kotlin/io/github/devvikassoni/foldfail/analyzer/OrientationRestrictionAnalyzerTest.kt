package io.github.devvikassoni.foldfail.analyzer

import io.github.devvikassoni.foldfail.core.Severity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class OrientationRestrictionAnalyzerTest {

    private val analyzer = OrientationRestrictionAnalyzer()

    @Test
    fun `detects portrait restriction`() {
        val manifest = createManifest(listOf("portrait"))

        try {
            val findings = analyzer.analyze(manifest)

            assertEquals(1, findings.size)
            assertEquals("FF001", findings.first().ruleId)
            assertEquals(Severity.WARNING, findings.first().severity)
        } finally {
            manifest.parentFile.deleteRecursively()
        }
    }

    @Test
    fun `detects multiple restricted activities`() {
        val manifest = createManifest(listOf("portrait", "landscape", "unspecified"))

        try {
            val findings = analyzer.analyze(manifest)

            assertEquals(2, findings.size)
            assertTrue(findings.all { it.ruleId == "FF001" })
        } finally {
            manifest.parentFile.deleteRecursively()
        }
    }

    @Test
    fun `does not report unspecified orientation`() {
        val manifest = createManifest(listOf("unspecified"))

        try {
            assertTrue(analyzer.analyze(manifest).isEmpty())
        } finally {
            manifest.parentFile.deleteRecursively()
        }
    }

    @Test
    fun `does not report activity without orientation attribute`() {
        val manifest = createManifest(emptyList())

        try {
            assertTrue(analyzer.analyze(manifest).isEmpty())
        } finally {
            manifest.parentFile.deleteRecursively()
        }
    }

    @Test
    fun `returns no findings for missing manifest`() {
        val missing = File(
            Files.createTempDirectory("foldfail-missing").toFile(),
            "AndroidManifest.xml"
        )

        try {
            assertTrue(analyzer.analyze(missing).isEmpty())
        } finally {
            missing.parentFile.deleteRecursively()
        }
    }

    @Test
    fun `reports error finding for malformed xml`() {
        val directory = Files.createTempDirectory("foldfail-malformed").toFile()
        val manifest = File(directory, "AndroidManifest.xml")
        manifest.writeText("<manifest><unclosed></manifest>")

        try {
            val findings = analyzer.analyze(manifest)
            assertEquals(1, findings.size)
            assertEquals(Severity.ERROR, findings.first().severity)
            assertEquals("FF001", findings.first().ruleId)
        } finally {
            directory.deleteRecursively()
        }
    }

    private fun createManifest(orientations: List<String?>): File {
        val directory = Files.createTempDirectory("foldfail-test").toFile()
        val manifest = File(directory, "AndroidManifest.xml")

        val activitiesXml = orientations.mapIndexed { index, orientation ->
            val attr = orientation?.let { """android:screenOrientation="$it"""" } ?: ""
            """<activity android:name=".Activity$index" $attr />"""
        }.joinToString("\n")

        manifest.writeText(
            """
            <manifest xmlns:android="http://schemas.android.com/apk/res/android">
                <application>
                    $activitiesXml
                </application>
            </manifest>
            """.trimIndent()
        )

        return manifest
    }
}
