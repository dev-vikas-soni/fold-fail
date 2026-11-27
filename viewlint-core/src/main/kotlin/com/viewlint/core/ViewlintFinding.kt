package com.viewlint.core

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

data class ViewlintFinding(
    val ruleId: String,
    val title: String,
    val message: String,
    val severity: Severity,
    val confidence: Confidence,
    val file: String,
    val evidence: String,
    val remediation: String
)

interface ViewlintRule {
    val id: String
    val description: String
    fun analyze(context: ViewlintContext): List<ViewlintFinding>
}
