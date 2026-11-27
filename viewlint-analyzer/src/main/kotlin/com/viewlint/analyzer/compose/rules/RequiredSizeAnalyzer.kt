package com.viewlint.analyzer.compose.rules

import com.viewlint.analyzer.compose.ComposeSourceParser
import com.viewlint.core.Confidence
import com.viewlint.core.ViewlintContext
import com.viewlint.core.ViewlintFinding
import com.viewlint.core.ViewlintRule
import com.viewlint.core.Severity

class RequiredSizeAnalyzer : ViewlintRule {

    override val id = "FF008"

    override val description =
        "Detects potentially restrictive uses of requiredWidth, requiredHeight, or requiredSize"

    override fun analyze(context: ViewlintContext): List<ViewlintFinding> {
        if (context.kotlinSources.isEmpty()) return emptyList()

        return buildList {
            for (sourcePath in context.kotlinSources) {
                val parsed = ComposeSourceParser.parse(sourcePath.toFile())
                if (parsed.error != null) continue

                for (comp in parsed.composables) {
                    for (call in comp.modifierCalls) {
                        if (call.contains("requiredWidth") || call.contains("requiredHeight") || call.contains("requiredSize")) {
                            add(
                                ViewlintFinding(
                                    ruleId = id,
                                    title = "Composable uses restrictive required size",
                                    message = "Composable '${comp.name}' uses required size modifier '$call', which can override incoming parent constraints and cause clipping on smaller or resized windows.",
                                    severity = Severity.WARNING,
                                    confidence = Confidence.MEDIUM,
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
}
