package io.github.devvikassoni.foldfail.core.reporting

import io.github.devvikassoni.foldfail.core.Confidence
import io.github.devvikassoni.foldfail.core.FoldFailFinding
import io.github.devvikassoni.foldfail.core.Severity
import io.github.devvikassoni.foldfail.core.reporting.baseline.FoldFailBaseline
import io.github.devvikassoni.foldfail.core.reporting.html.HtmlReportWriter
import io.github.devvikassoni.foldfail.core.reporting.json.JsonReportWriter
import io.github.devvikassoni.foldfail.core.reporting.sarif.SarifReportWriter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class ReportingTest {

    private val sampleFinding = FoldFailFinding(
        ruleId = "FF001",
        title = "Activity orientation is restricted",
        message = "MainActivity restricts orientation to 'portrait'.",
        severity = Severity.WARNING,
        confidence = Confidence.MEDIUM,
        file = "AndroidManifest.xml",
        evidence = "android:screenOrientation=\"portrait\"",
        remediation = "Review orientation restriction."
    )

    private val sampleReport = FoldFailReport(
        metadata = FoldFailReportMetadata(
            generatedAt = "2026-03-30T00:00:00Z",
            projectName = "test-project"
        ),
        summary = FoldFailSummary(
            totalFindings = 1,
            errorCount = 0,
            warningCount = 1,
            infoCount = 0,
            byRule = mapOf("FF001" to 1)
        ),
        findings = listOf(sampleFinding)
    )

    @Test
    fun `JsonReportWriter generates valid json`() {
        val json = JsonReportWriter.write(sampleReport)
        assertTrue(json.contains("\"ruleId\": \"FF001\""))
        assertTrue(json.contains("\"projectName\": \"test-project\""))
    }

    @Test
    fun `HtmlReportWriter generates valid html`() {
        val html = HtmlReportWriter.write(sampleReport)
        assertTrue(html.contains("<!DOCTYPE html>"))
        assertTrue(html.contains("FF001"))
        assertTrue(html.contains("test-project"))
    }

    @Test
    fun `SarifReportWriter generates valid sarif`() {
        val sarif = SarifReportWriter.write(sampleReport)
        assertTrue(sarif.contains("sarif-schema-2.1.0.json"))
        assertTrue(sarif.contains("FF001"))
    }

    @Test
    fun `FoldFailBaseline creates and filters new findings`() {
        val dir = Files.createTempDirectory("baseline-test").toFile()
        val baselineFile = File(dir, "baseline.txt")

        try {
            FoldFailBaseline.saveBaseline(baselineFile, listOf(sampleFinding))
            val signatures = FoldFailBaseline.loadBaseline(baselineFile)
            assertEquals(1, signatures.size)

            val newFinding = FoldFailFinding(
                ruleId = "FF002",
                title = "Aspect ratio restricted",
                message = "Details...",
                severity = Severity.WARNING,
                confidence = Confidence.MEDIUM,
                file = "AndroidManifest.xml",
                evidence = "android:maxAspectRatio=\"2.4\"",
                remediation = "Review."
            )

            val filtered = FoldFailBaseline.filterNewFindings(listOf(sampleFinding, newFinding), signatures)
            assertEquals(1, filtered.size)
            assertEquals("FF002", filtered.first().ruleId)
        } finally {
            dir.deleteRecursively()
        }
    }
}
