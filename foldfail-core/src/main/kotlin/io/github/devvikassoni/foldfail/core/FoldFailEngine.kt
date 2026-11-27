package io.github.devvikassoni.foldfail.core

class FoldFailEngine(
    private val rules: List<FoldFailRule>
) {

    fun analyze(context: FoldFailContext): List<FoldFailFinding> {
        val findings = mutableListOf<FoldFailFinding>()
        for (rule in rules) {
            try {
                findings.addAll(rule.analyze(context))
            } catch (e: Exception) {
                findings.add(
                    FoldFailFinding(
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