package com.viewlint.core.reporting.baseline

import com.viewlint.core.ViewlintFinding
import java.io.File

object ViewlintBaseline {

    fun computeSignature(finding: ViewlintFinding): String {
        return "${finding.ruleId}:${finding.file}:${finding.evidence}"
    }

    fun saveBaseline(baselineFile: File, findings: List<ViewlintFinding>) {
        val signatures = findings.map { computeSignature(it) }.sorted().distinct()
        baselineFile.parentFile?.mkdirs()
        baselineFile.writeText(signatures.joinToString("\n"))
    }

    fun loadBaseline(baselineFile: File): Set<String> {
        if (!baselineFile.exists() || !baselineFile.isFile) {
            return emptySet()
        }
        return baselineFile.readLines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toSet()
    }

    fun filterNewFindings(findings: List<ViewlintFinding>, baselineSignatures: Set<String>): List<ViewlintFinding> {
        return findings.filter { finding ->
            computeSignature(finding) !in baselineSignatures
        }
    }
}
