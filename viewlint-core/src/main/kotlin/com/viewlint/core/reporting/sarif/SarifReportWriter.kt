package com.viewlint.core.reporting.sarif

import com.viewlint.core.reporting.ViewlintReport

object SarifReportWriter {

    fun write(report: ViewlintReport): String {
        val sb = StringBuilder()
        sb.append("{\n")
        sb.append("  \"\$schema\": \"https://docs.oasis-open.org/sarif/sarif/v2.1.0/os/schemas/sarif-schema-2.1.0.json\",\n")
        sb.append("  \"version\": \"2.1.0\",\n")
        sb.append("  \"runs\": [\n")
        sb.append("    {\n")
        sb.append("      \"tool\": {\n")
        sb.append("        \"driver\": {\n")
        sb.append("          \"name\": \"${report.metadata.toolName}\",\n")
        sb.append("          \"version\": \"${report.metadata.toolVersion}\",\n")
        sb.append("          \"rules\": [\n")

        val uniqueRules = report.findings.map { it.ruleId }.distinct().sorted()
        uniqueRules.forEachIndexed { index, ruleId ->
            val finding = report.findings.first { it.ruleId == ruleId }
            sb.append("            {\n")
            sb.append("              \"id\": \"${escapeJson(ruleId)}\",\n")
            sb.append("              \"name\": \"${escapeJson(finding.title)}\",\n")
            sb.append("              \"shortDescription\": {\n")
            sb.append("                \"text\": \"${escapeJson(finding.title)}\"\n")
            sb.append("              },\n")
            sb.append("              \"fullDescription\": {\n")
            sb.append("                \"text\": \"${escapeJson(finding.message)}\"\n")
            sb.append("              }\n")
            sb.append("            }")
            if (index < uniqueRules.size - 1) sb.append(",")
            sb.append("\n")
        }

        sb.append("          ]\n")
        sb.append("        }\n")
        sb.append("      },\n")
        sb.append("      \"results\": [\n")

        val sortedFindings = report.findings.sortedWith(compareBy({ it.ruleId }, { it.file }, { it.evidence }))
        sortedFindings.forEachIndexed { index, finding ->
            val level = when (finding.severity.name) {
                "ERROR" -> "error"
                "WARNING" -> "warning"
                else -> "note"
            }
            val relativePath = normalizePath(finding.file)
            val fingerprint = "${finding.ruleId}:${finding.file}:${finding.evidence}".hashCode().toUInt().toString(16)

            sb.append("        {\n")
            sb.append("          \"ruleId\": \"${escapeJson(finding.ruleId)}\",\n")
            sb.append("          \"level\": \"$level\",\n")
            sb.append("          \"message\": {\n")
            sb.append("            \"text\": \"${escapeJson(finding.message)} Remediation: ${escapeJson(finding.remediation)}\"\n")
            sb.append("          },\n")
            sb.append("          \"partialFingerprints\": {\n")
            sb.append("            \"primaryLocationLineHash\": \"$fingerprint\"\n")
            sb.append("          },\n")
            sb.append("          \"locations\": [\n")
            sb.append("            {\n")
            sb.append("              \"physicalLocation\": {\n")
            sb.append("                \"artifactLocation\": {\n")
            sb.append("                  \"uri\": \"${escapeJson(relativePath)}\"\n")
            sb.append("                }\n")
            sb.append("              }\n")
            sb.append("            }\n")
            sb.append("          ]\n")
            sb.append("        }")
            if (index < sortedFindings.size - 1) sb.append(",")
            sb.append("\n")
        }

        sb.append("      ]\n")
        sb.append("    }\n")
        sb.append("  ]\n")
        sb.append("}\n")
        return sb.toString()
    }

    private fun normalizePath(filePath: String): String {
        var normalized = filePath.replace("\\", "/")
        if (normalized.contains("/viewlint/")) {
            normalized = normalized.substringAfter("/viewlint/")
        } else if (normalized.contains("/fold-fail/")) {
            normalized = normalized.substringAfter("/fold-fail/")
        } else if (normalized.startsWith("/")) {
            val parts = normalized.split("/")
            if (parts.size > 3) {
                normalized = parts.takeLast(3).joinToString("/")
            }
        }
        return normalized
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
