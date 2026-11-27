package com.viewlint.core

class ViewlintEngine(
    private val rules: List<ViewlintRule>
) {

    fun analyze(context: ViewlintContext): List<ViewlintFinding> {
        val findings = mutableListOf<ViewlintFinding>()
        for (rule in rules) {
            try {
                findings.addAll(rule.analyze(context))
            } catch (e: Exception) {
                findings.add(
                    ViewlintFinding(
                        ruleId = "SYSTEM",
                        title = "Rule execution failed",
                        message = "Rule ${rule.id} (${rule.javaClass.simpleName}) threw an exception: ${e.message}",
                        severity = Severity.ERROR,
                        confidence = Confidence.HIGH,
                        file = context.projectRoot.toString(),
                        evidence = e.stackTraceToString().take(500),
                        remediation = "Inspect stack trace and fix rule analysis logic or malformed input."
                    )
                )
            }
        }
        return findings.sortedWith(
            compareBy(
                { it.ruleId },
                { it.file }
            )
        )
    }
}
