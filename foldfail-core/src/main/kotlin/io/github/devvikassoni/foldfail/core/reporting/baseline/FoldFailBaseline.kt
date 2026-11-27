package io.github.devvikassoni.foldfail.core.reporting.baseline

import io.github.devvikassoni.foldfail.core.FoldFailFinding
import java.io.File

object FoldFailBaseline {

    fun computeSignature(finding: FoldFailFinding): String {
        return "${finding.ruleId}:${finding.file}:${finding.evidence}"
    }

    fun saveBaseline(baselineFile: File, findings: List<FoldFailFinding>) {
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

    fun filterNewFindings(findings: List<FoldFailFinding>, baselineSignatures: Set<String>): List<FoldFailFinding> {
        return findings.filter { finding ->
            computeSignature(finding) !in baselineSignatures
        }
    }
}
