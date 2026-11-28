package com.viewlint.core.reporting.json

import com.viewlint.core.reporting.ViewlintReport

object JsonReportWriter {

    fun write(report: ViewlintReport): String {
        val sb = StringBuilder()
        sb.append("{\n")
        sb.append("  \"schemaVersion\": \"${report.metadata.schemaVersion}\",\n")
        sb.append("  \"toolName\": \"${report.metadata.toolName}\",\n")
        sb.append("  \"toolVersion\": \"${report.metadata.toolVersion}\",\n")
        sb.append("  \"generatedAt\": \"${report.metadata.generatedAt}\",\n")
        sb.append("  \"projectName\": \"${escapeJson(report.metadata.projectName)}\",\n")
        sb.append("  \"summary\": {\n")
        sb.append("    \"totalFindings\": ${report.summary.totalFindings},\n")
        sb.append("    \"errorCount\": ${report.summary.errorCount},\n")
        sb.append("    \"warningCount\": ${report.summary.warningCount},\n")
        sb.append("    \"infoCount\": ${report.summary.infoCount},\n")
        sb.append("    \"byRule\": {\n")
        val sortedRules = report.summary.byRule.entries.sortedBy { it.key }
        sortedRules.forEachIndexed { index, entry ->
            sb.append("      \"${entry.key}\": ${entry.value}")
            if (index < sortedRules.size - 1) sb.append(",")
            sb.append("\n")
        }
        sb.append("    }\n")
        sb.append("  },\n")
        sb.append("  \"findings\": [\n")
        val sortedFindings = report.findings.sortedWith(compareBy({ it.ruleId }, { it.file }, { it.evidence }))
        sortedFindings.forEachIndexed { index, finding ->
            sb.append("    {\n")
            sb.append("      \"ruleId\": \"${escapeJson(finding.ruleId)}\",\n")
            sb.append("      \"title\": \"${escapeJson(finding.title)}\",\n")
            sb.append("      \"message\": \"${escapeJson(finding.message)}\",\n")
            sb.append("      \"severity\": \"${finding.severity}\",\n")
            sb.append("      \"confidence\": \"${finding.confidence}\",\n")
            sb.append("      \"priority\": \"${finding.priority}\",\n")
            sb.append("      \"file\": \"${escapeJson(finding.file)}\",\n")
            sb.append("      \"evidence\": \"${escapeJson(finding.evidence)}\",\n")
            sb.append("      \"remediation\": \"${escapeJson(finding.remediation)}\"\n")
            sb.append("    }")
            if (index < sortedFindings.size - 1) sb.append(",")
            sb.append("\n")
        }
        sb.append("  ]\n")
        sb.append("}\n")
        return sb.toString()
    }

    private fun escapeJson(input: String): String {
        return input
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\b", "\\b")
            .replace("\u000C", "\\f")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }
}
