package io.github.devvikassoni.foldfail.core

enum class Severity {
    INFO,
    WARNING,
    ERROR
}

enum class Confidence {
    LOW,
    MEDIUM,
    HIGH
}

data class FoldFailFinding(
    val ruleId: String,
    val title: String,
    val message: String,
    val severity: Severity,
    val confidence: Confidence,
    val file: String,
    val evidence: String,
    val remediation: String
)

interface FoldFailRule {
    val id: String
    val description: String
    fun analyze(context: FoldFailContext): List<FoldFailFinding>
}