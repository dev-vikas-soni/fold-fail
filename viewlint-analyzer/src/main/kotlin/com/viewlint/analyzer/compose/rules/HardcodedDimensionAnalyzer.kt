package com.viewlint.analyzer.compose.rules

import com.viewlint.analyzer.compose.ComposeSourceParser
import com.viewlint.core.Confidence
import com.viewlint.core.ViewlintContext
import com.viewlint.core.ViewlintFinding
import com.viewlint.core.ViewlintRule
import com.viewlint.core.Severity

class HardcodedDimensionAnalyzer : ViewlintRule {

    override val id = "FF007"

    override val description =
        "Detects potentially restrictive hardcoded dimensions in Compose modifier chains"

    override fun analyze(context: ViewlintContext): List<ViewlintFinding> {
        if (context.kotlinSources.isEmpty()) return emptyList()

        return buildList {
            for (sourcePath in context.kotlinSources) {
                val parsed = ComposeSourceParser.parse(sourcePath.toFile())
                if (parsed.error != null) continue

                for (comp in parsed.composables) {
                    if (comp.hasAdaptiveCues) continue

                    for (call in comp.modifierCalls) {
                        if ((call.contains("width") || call.contains("height") || call.contains("size")) &&
                            !call.contains("required") &&
                            call.contains("dp")) {

                            add(
                                ViewlintFinding(
                                    ruleId = id,
                                    title = "Composable uses hardcoded dimensions",
                                    message = "Composable '${comp.name}' uses fixed dimension modifier '$call' without detectable adaptive branching.",
                                    severity = Severity.WARNING,
                                    confidence = Confidence.MEDIUM,
                                    file = sourcePath.toString(),
                                    evidence = call,
                                    remediation = "Use flexible modifiers (e.g. fillMaxWidth, weight, wrapContentWidth) or adapt to WindowSizeClass / BoxWithConstraints for foldables and tablets."
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
