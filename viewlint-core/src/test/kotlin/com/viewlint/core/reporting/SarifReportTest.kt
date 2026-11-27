package com.viewlint.core.reporting

import com.viewlint.core.Confidence
import com.viewlint.core.ViewlintFinding
import com.viewlint.core.Severity
import com.viewlint.core.reporting.sarif.SarifReportWriter
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class SarifReportTest {

    @Test
    fun `SarifReportWriter normalizes paths and includes partialFingerprints`() {
        val finding = ViewlintFinding(
            ruleId = "FF001",
            title = "Activity orientation restricted",
            message = "MainActivity restricts orientation.",
            severity = Severity.WARNING,
            confidence = Confidence.MEDIUM,
            file = "/Users/developer/viewlint/app/src/main/AndroidManifest.xml",
            evidence = "android:screenOrientation=\"portrait\"",
            remediation = "Review."
        )

        val report = ViewlintReport(
            metadata = ViewlintReportMetadata(
                generatedAt = Instant.now().toString(),
                projectName = "test-project"
            ),
            summary = ViewlintSummary(
                totalFindings = 1,
                errorCount = 0,
                warningCount = 1,
                infoCount = 0,
                byRule = mapOf("FF001" to 1)
            ),
            findings = listOf(finding)
        )

        val sarifJson = SarifReportWriter.write(report)

        assertTrue("SARIF should contain schema", sarifJson.contains("sarif-schema-2.1.0.json"))
        assertTrue("SARIF should contain ruleId", sarifJson.contains("FF001"))
        assertTrue("SARIF should contain partialFingerprints", sarifJson.contains("partialFingerprints"))
        assertTrue("SARIF should normalize path relative to project root", sarifJson.contains("app/src/main/AndroidManifest.xml"))
    }
}
