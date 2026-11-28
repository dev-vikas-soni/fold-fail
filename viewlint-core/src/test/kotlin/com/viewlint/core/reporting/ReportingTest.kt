package com.viewlint.core.reporting

import com.viewlint.core.Confidence
import com.viewlint.core.Severity
import com.viewlint.core.ViewlintFinding
import com.viewlint.core.reporting.baseline.ViewlintBaseline
import com.viewlint.core.reporting.html.HtmlReportWriter
import com.viewlint.core.reporting.json.JsonReportWriter
import com.viewlint.core.reporting.sarif.SarifReportWriter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import java.io.File
import java.nio.file.Files
import org.junit.Test

class ReportingTest {

    private val sampleFinding = ViewlintFinding(
        ruleId = "FF001",
        title = "Activity orientation is restricted",
        message = "MainActivity restricts orientation to 'portrait' & <script>alert('xss')</script>.",
        severity = Severity.WARNING,
        confidence = Confidence.HIGH,
        file = "src/main/AndroidManifest.xml",
        evidence = "android:screenOrientation=\"portrait\"",
        remediation = "Review orientation restriction."
    )

    private val sampleReport = ViewlintReport(
        metadata = ViewlintReportMetadata(
            generatedAt = "2026-03-30T00:00:00Z",
            projectName = "test-project"
        ),
        summary = ViewlintSummary(
            totalFindings = 1,
            errorCount = 0,
            warningCount = 1,
            infoCount = 0,
            byRule = mapOf("FF001" to 1)
        ),
        findings = listOf(sampleFinding)
    )

    @Test
    fun `JsonReportWriter generates valid json with priority and string escaping`() {
        val json = JsonReportWriter.write(sampleReport)
        assertTrue(json.contains("\"ruleId\": \"FF001\""))
        assertTrue(json.contains("\"projectName\": \"test-project\""))
        assertTrue(json.contains("\"confidence\": \"HIGH\""))
        assertTrue(json.contains("\"priority\": \"HIGH\""))
        assertTrue(json.contains("toolVersion\": \"1.0.0\""))
    }

    @Test
    fun `HtmlReportWriter generates modern light theme html report with escaping and authoritative version`() {
        val html = HtmlReportWriter.write(sampleReport)
        assertTrue(html.contains("<!DOCTYPE html>"))
        assertTrue(html.contains("Viewlint"))
        assertTrue(html.contains("Adaptive Quality Report"))
        assertTrue(html.contains("test-project"))
        assertTrue(html.contains("FF001"))
        assertTrue(html.contains("HIGH CONFIDENCE"))
        assertTrue(html.contains("HIGH PRIORITY"))
        assertTrue(html.contains("Viewlint v1.0.0"))
        assertTrue(html.contains("--bg-page: #F8FAFC"))
        assertTrue(html.contains("--bg-surface: #FFFFFF"))
        assertTrue(html.contains("Top Adaptive Risks"))
        assertTrue(html.contains("Screen & File Risk Summaries"))
        assertTrue(html.contains("searchInput"))
        assertTrue(html.contains("controls-bar"))
        assertTrue(html.contains("MEDIUM ADAPTIVE RISK"))

        // Verify HTML escaping of special characters
        assertTrue("Script tag should be HTML escaped", html.contains("&lt;script&gt;"))
        assertFalse("Unescaped script tag should not exist in HTML", html.contains("<script>alert"))

        // Verify offline rendering (no external CDN scripts or remote fonts)
        assertFalse(html.contains("https://cdn."))
        assertFalse(html.contains("https://cdnjs."))
        assertFalse(html.contains("https://stackpath."))
    }

    @Test
    fun `SarifReportWriter generates valid sarif`() {
        val sarif = SarifReportWriter.write(sampleReport)
        assertTrue(sarif.contains("sarif-schema-2.1.0.json"))
        assertTrue(sarif.contains("FF001"))
        assertTrue(sarif.contains("1.0.0"))
    }

    @Test
    fun `ViewlintBaseline creates and filters new findings`() {
        val dir = Files.createTempDirectory("baseline-test").toFile()
        val baselineFile = File(dir, "baseline.txt")

        try {
            ViewlintBaseline.saveBaseline(baselineFile, listOf(sampleFinding))
            val signatures = ViewlintBaseline.loadBaseline(baselineFile)
            assertEquals(1, signatures.size)

            val newFinding = ViewlintFinding(
                ruleId = "FF002",
                title = "Aspect ratio restricted",
                message = "Details...",
                severity = Severity.WARNING,
                confidence = Confidence.MEDIUM,
                file = "src/main/AndroidManifest.xml",
                evidence = "android:maxAspectRatio=\"2.4\"",
                remediation = "Review."
            )

            val filtered = ViewlintBaseline.filterNewFindings(listOf(sampleFinding, newFinding), signatures)
            assertEquals(1, filtered.size)
            assertEquals("FF002", filtered.first().ruleId)
        } finally {
            dir.deleteRecursively()
        }
    }
}
