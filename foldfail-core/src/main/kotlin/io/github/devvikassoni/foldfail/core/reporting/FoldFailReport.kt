package io.github.devvikassoni.foldfail.core.reporting

import io.github.devvikassoni.foldfail.core.FoldFailFinding

enum class ReportFormat {
    CONSOLE,
    JSON,
    HTML,
    SARIF
}

enum class QualityGateLevel {
    NEVER,
    ERROR,
    WARNING,
    NEW
}

data class FoldFailReportMetadata(
    val schemaVersion: String = "1.0",
    val toolName: String = "FoldFail",
    val toolVersion: String = "0.5.0",
    val generatedAt: String,
    val projectName: String
)

data class FoldFailSummary(
    val totalFindings: Int,
    val errorCount: Int,
    val warningCount: Int,
    val infoCount: Int,
    val byRule: Map<String, Int>
)

data class FoldFailReport(
    val metadata: FoldFailReportMetadata,
    val summary: FoldFailSummary,
    val findings: List<FoldFailFinding>
)
