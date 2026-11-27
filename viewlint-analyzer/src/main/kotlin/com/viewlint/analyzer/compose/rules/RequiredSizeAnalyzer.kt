package com.viewlint.analyzer.compose.rules

import com.viewlint.analyzer.compose.ComposeSourceParser
import com.viewlint.core.Confidence
import com.viewlint.core.Severity
import com.viewlint.core.ViewlintContext
import com.viewlint.core.ViewlintFinding
import com.viewlint.core.ViewlintRule

class RequiredSizeAnalyzer : ViewlintRule {

    override val id = "FF008"

    override val description =
        "Detects potentially restrictive uses of requiredWidth, requiredHeight, or requiredSize that override parent constraints"

    override fun analyze(context: ViewlintContext): List<ViewlintFinding> {
        if (context.kotlinSources.isEmpty()) return emptyList()

        return buildList {
            for (sourcePath in context.kotlinSources) {
                val parsed = ComposeSourceParser.parse(sourcePath.toFile())
                if (parsed.error != null) continue

                for (comp in parsed.composables) {
                    for (call in comp.modifierCalls) {
                        if (call.contains("requiredWidth") || call.contains("requiredHeight") || call.contains("requiredSize")) {
                            val dpValue = parseDpValue(call) ?: 0.0

                            // Assign Confidence: HIGH for container/panel required sizes (>48dp), LOW for small icons/badges
                            val confidence = if (dpValue > 48.0 || comp.isScreenOrContainer) Confidence.HIGH else Confidence.LOW

                            add(
                                ViewlintFinding(
                                    ruleId = id,
                                    title = "Composable uses restrictive required size",
                                    message = "Composable '${comp.name}' uses required size modifier '$call', which overrides incoming parent layout constraints and risks clipping on smaller or resized windows.",
                                    severity = Severity.WARNING,
                                    confidence = confidence,
                                    file = sourcePath.toString(),
                                    evidence = call,
                                    remediation = "Use standard width, height, or size modifiers instead of required variants unless overriding parent constraints is strictly necessary."
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    private fun parseDpValue(call: String): Double? {
        val match = Regex("(?:requiredWidth|requiredHeight|requiredSize)\\s*\\(\\s*([0-9]+(?:\\.[0-9]+)?)\\s*\\.\\s*dp").find(call)
            ?: Regex("([0-9]+(?:\\.[0-9]+)?)\\s*\\.\\s*dp").find(call)
        return match?.groupValues?.get(1)?.toDoubleOrNull()
    }
}
