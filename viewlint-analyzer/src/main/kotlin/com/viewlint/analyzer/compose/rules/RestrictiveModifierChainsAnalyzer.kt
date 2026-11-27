package com.viewlint.analyzer.compose.rules

import com.viewlint.analyzer.compose.ComposeSourceParser
import com.viewlint.core.Confidence
import com.viewlint.core.ViewlintContext
import com.viewlint.core.ViewlintFinding
import com.viewlint.core.ViewlintRule
import com.viewlint.core.Severity

class RestrictiveModifierChainsAnalyzer : ViewlintRule {

    override val id = "FF010"

    override val description =
        "Identify combinations of fixed-size modifiers that may constrain available space in resizable or larger-screen configurations"

    override fun analyze(context: ViewlintContext): List<ViewlintFinding> {
        if (context.kotlinSources.isEmpty()) return emptyList()

        return buildList {
            for (sourcePath in context.kotlinSources) {
                val parsed = ComposeSourceParser.parse(sourcePath.toFile())
                if (parsed.error != null) continue

                for (comp in parsed.composables) {
                    if (comp.hasAdaptiveCues) continue

                    val hasWidth = comp.modifierCalls.any { it.contains("width") && it.contains("dp") }
                    val hasHeight = comp.modifierCalls.any { it.contains("height") && it.contains("dp") }
                    val hasSize = comp.modifierCalls.any { it.contains("size") && it.contains("dp") }

                    if ((hasWidth && hasHeight) || hasSize) {
                        add(
                            ViewlintFinding(
                                ruleId = id,
                                title = "Restrictive modifier chain combination",
                                message = "Composable '${comp.name}' combines fixed width and height or size modifiers, heavily constraining space across screen size changes.",
                                severity = Severity.WARNING,
                                confidence = Confidence.MEDIUM,
                                file = sourcePath.toString(),
                                evidence = comp.modifierCalls.filter { it.contains("dp") }.joinToString(", "),
                                remediation = "Avoid locking both width and height to fixed dp values. Use weight, fillMaxSize, or responsive sizing constraints."
                            )
                        )
                    }
                }
            }
        }
    }
}
