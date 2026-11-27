package com.viewlint.analyzer

import com.viewlint.core.Severity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class AspectRatioAnalyzerTest {

    private val analyzer = AspectRatioAnalyzer()

    @Test
    fun `targetSdkVersion 23 attribute omitted results in effective false and reports restriction`() {
        val manifest = createManifestWithSdk(
            targetSdk = "23",
            maxAspect = "2.4",
            minAspect = null,
            resizeable = null,
            appResizeable = null
        )

        try {
            val findings = analyzer.analyze(manifest)
            assertEquals(1, findings.size)
            assertEquals("FF002", findings.first().ruleId)
            assertEquals(Severity.WARNING, findings.first().severity)
        } finally {
            manifest.parentFile.deleteRecursively()
        }
    }

    @Test
    fun `targetSdkVersion 24 attribute omitted results in effective true and ignores restriction`() {
        val manifest = createManifestWithSdk(
            targetSdk = "24",
            maxAspect = "2.4",
            minAspect = "1.2",
            resizeable = null,
            appResizeable = null
        )

        try {
            assertTrue(analyzer.analyze(manifest).isEmpty())
        } finally {
            manifest.parentFile.deleteRecursively()
        }
    }

    @Test
    fun `targetSdkVersion 35 attribute omitted results in effective true and ignores restriction`() {
        val manifest = createManifestWithSdk(
            targetSdk = "35",
            maxAspect = "2.4",
            minAspect = "1.2",
            resizeable = null,
            appResizeable = null
        )

        try {
            assertTrue(analyzer.analyze(manifest).isEmpty())
        } finally {
            manifest.parentFile.deleteRecursively()
        }
    }

    @Test
    fun `targetSdkVersion 23 application-level true results in effective true and ignores restriction`() {
        val manifest = createManifestWithSdk(
            targetSdk = "23",
            maxAspect = "2.4",
            minAspect = null,
            resizeable = null,
            appResizeable = "true"
        )

        try {
            assertTrue(analyzer.analyze(manifest).isEmpty())
        } finally {
            manifest.parentFile.deleteRecursively()
        }
    }

    @Test
    fun `targetSdkVersion 35 application-level false results in effective false and reports restriction`() {
        val manifest = createManifestWithSdk(
            targetSdk = "35",
            maxAspect = "2.4",
            minAspect = null,
            resizeable = null,
            appResizeable = "false"
        )

        try {
            val findings = analyzer.analyze(manifest)
            assertEquals(1, findings.size)
            assertEquals("FF002", findings.first().ruleId)
        } finally {
            manifest.parentFile.deleteRecursively()
        }
    }

    @Test
    fun `activity-level true overrides application-level false`() {
        val manifest = createManifestWithSdk(
            targetSdk = "35",
            maxAspect = "2.4",
            minAspect = null,
            resizeable = "true",
            appResizeable = "false"
        )

        try {
            assertTrue(analyzer.analyze(manifest).isEmpty())
        } finally {
            manifest.parentFile.deleteRecursively()
        }
    }

    @Test
    fun `activity-level false overrides application-level true`() {
        val manifest = createManifestWithSdk(
            targetSdk = "35",
            maxAspect = "2.4",
            minAspect = "1.2",
            resizeable = "false",
            appResizeable = "true"
        )

        try {
            val findings = analyzer.analyze(manifest)
            assertEquals(2, findings.size)
            assertTrue(findings.all { it.ruleId == "FF002" })
        } finally {
            manifest.parentFile.deleteRecursively()
        }
    }

    @Test
    fun `verifies both maxAspectRatio and minAspectRatio findings when non-resizable`() {
        val manifest = createManifestWithSdk(
            targetSdk = "23",
            maxAspect = "2.4",
            minAspect = "1.2",
            resizeable = "false",
            appResizeable = null
        )

        try {
            val findings = analyzer.analyze(manifest)
            assertEquals(2, findings.size)
            assertTrue(findings.any { it.evidence.contains("maxAspectRatio") })
            assertTrue(findings.any { it.evidence.contains("minAspectRatio") })
        } finally {
            manifest.parentFile.deleteRecursively()
        }
    }

    @Test
    fun `omitted targetSdkVersion conservatively defaults to false and reports restriction`() {
        val manifest = createManifestWithSdk(
            targetSdk = null,
            maxAspect = "2.4",
            minAspect = null,
            resizeable = null,
            appResizeable = null
        )

        try {
            val findings = analyzer.analyze(manifest)
            assertEquals(1, findings.size)
            assertEquals("FF002", findings.first().ruleId)
        } finally {
            manifest.parentFile.deleteRecursively()
        }
    }

    @Test
    fun `reports error finding for malformed xml`() {
        val directory = Files.createTempDirectory("viewlint-malformed-ar").toFile()
        val manifest = File(directory, "AndroidManifest.xml")
        manifest.writeText("<manifest><unclosed></manifest>")

        try {
            val findings = analyzer.analyze(manifest)
            assertEquals(1, findings.size)
            assertEquals(Severity.ERROR, findings.first().severity)
            assertEquals("FF002", findings.first().ruleId)
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun `returns no findings for missing manifest`() {
        val missing = File(
            Files.createTempDirectory("viewlint-missing-ar").toFile(),
            "AndroidManifest.xml"
        )

        try {
            assertTrue(analyzer.analyze(missing).isEmpty())
        } finally {
            missing.parentFile.deleteRecursively()
        }
    }

    private fun createManifestWithSdk(
        targetSdk: String?,
        maxAspect: String?,
        minAspect: String?,
        resizeable: String?,
        appResizeable: String?
    ): File {
        val directory = Files.createTempDirectory("viewlint-test-ar").toFile()
        val manifest = File(directory, "AndroidManifest.xml")

        val usesSdkTag = targetSdk?.let { """<uses-sdk android:targetSdkVersion="$it" />""" } ?: ""
        val maxAttr = maxAspect?.let { """android:maxAspectRatio="$it"""" } ?: ""
        val minAttr = minAspect?.let { """android:minAspectRatio="$it"""" } ?: ""
        val resizeAttr = resizeable?.let { """android:resizeableActivity="$it"""" } ?: ""
        val appResizeAttr = appResizeable?.let { """android:resizeableActivity="$it"""" } ?: ""

        manifest.writeText(
            """
            <manifest xmlns:android="http://schemas.android.com/apk/res/android">
                $usesSdkTag
                <application $appResizeAttr>
                    <activity
                        android:name=".MainActivity"
                        $maxAttr
                        $minAttr
                        $resizeAttr />
                </application>
            </manifest>
            """.trimIndent()
        )

        return manifest
    }
}
