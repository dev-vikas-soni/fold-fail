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

enum class Priority {
    CRITICAL,
    HIGH,
    MEDIUM,
    LOW
}

data class ViewlintFinding(
    val ruleId: String,
    val title: String,
    val message: String,
    val severity: Severity,
    val confidence: Confidence,
    val file: String,
    val evidence: String,
    val remediation: String,
    val priority: Priority = calculatePriority(severity, confidence)
) {
    companion object {
        fun calculatePriority(severity: Severity, confidence: Confidence): Priority {
            return when {
                severity == Severity.ERROR && confidence == Confidence.HIGH -> Priority.CRITICAL
                severity == Severity.ERROR -> Priority.HIGH
                severity == Severity.WARNING && confidence == Confidence.HIGH -> Priority.HIGH
                severity == Severity.WARNING && confidence == Confidence.MEDIUM -> Priority.MEDIUM
                severity == Severity.WARNING -> Priority.LOW
                else -> Priority.LOW
            }
        }
    }
}

interface ViewlintRule {
    val id: String
    val description: String
    fun analyze(context: ViewlintContext): List<ViewlintFinding>
}
