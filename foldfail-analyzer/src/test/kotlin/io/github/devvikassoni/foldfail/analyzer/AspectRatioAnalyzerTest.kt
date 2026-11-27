package io.github.devvikassoni.foldfail.analyzer

import io.github.devvikassoni.foldfail.core.Severity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class AspectRatioAnalyzerTest {

    private val analyzer = AspectRatioAnalyzer()

    @Test
    fun `detects maxAspectRatio restriction`() {
        val manifest = createManifest(maxAspect = "2.4", minAspect = null, resizeable = null, appResizeable = null)

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
    fun `detects minAspectRatio restriction`() {
        val manifest = createManifest(maxAspect = null, minAspect = "1.2", resizeable = "false", appResizeable = null)

        try {
            val findings = analyzer.analyze(manifest)
            assertEquals(1, findings.size)
            assertEquals("FF002", findings.first().ruleId)
        } finally {
            manifest.parentFile.deleteRecursively()
        }
    }

    @Test
    fun `does not report when resizeableActivity is true`() {
        val manifest = createManifest(maxAspect = "2.4", minAspect = null, resizeable = "true", appResizeable = null)

        try {
            assertTrue(analyzer.analyze(manifest).isEmpty())
        } finally {
            manifest.parentFile.deleteRecursively()
        }
    }

    @Test
    fun `does not report when app-level resizeableActivity is true`() {
        val manifest = createManifest(maxAspect = "2.4", minAspect = null, resizeable = null, appResizeable = "true")

        try {
            assertTrue(analyzer.analyze(manifest).isEmpty())
        } finally {
            manifest.parentFile.deleteRecursively()
        }
    }

    @Test
    fun `reports error finding for malformed xml`() {
        val directory = Files.createTempDirectory("foldfail-malformed-ar").toFile()
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
            Files.createTempDirectory("foldfail-missing-ar").toFile(),
            "AndroidManifest.xml"
        )

        try {
            assertTrue(analyzer.analyze(missing).isEmpty())
        } finally {
            missing.parentFile.deleteRecursively()
        }
    }

    private fun createManifest(
        maxAspect: String?,
        minAspect: String?,
        resizeable: String?,
        appResizeable: String?
    ): File {
        val directory = Files.createTempDirectory("foldfail-test-ar").toFile()
        val manifest = File(directory, "AndroidManifest.xml")

        val maxAttr = maxAspect?.let { """android:maxAspectRatio="$it"""" } ?: ""
        val minAttr = minAspect?.let { """android:minAspectRatio="$it"""" } ?: ""
        val resizeAttr = resizeable?.let { """android:resizeableActivity="$it"""" } ?: ""
        val appResizeAttr = appResizeable?.let { """android:resizeableActivity="$it"""" } ?: ""

        manifest.writeText(
            """
            <manifest xmlns:android="http://schemas.android.com/apk/res/android">
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
