package com.viewlint.core.reporting

import com.viewlint.core.ViewlintFinding

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

data class ViewlintReportMetadata(
    val schemaVersion: String = "1.0",
    val toolName: String = "Viewlint",
    val toolVersion: String = "0.7.1",
    val generatedAt: String,
    val projectName: String
)

data class ViewlintSummary(
    val totalFindings: Int,
    val errorCount: Int,
    val warningCount: Int,
    val infoCount: Int,
    val byRule: Map<String, Int>
)

data class ViewlintReport(
    val metadata: ViewlintReportMetadata,
    val summary: ViewlintSummary,
    val findings: List<ViewlintFinding>
)
