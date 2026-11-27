package io.github.devvikassoni.foldfail.analyzer.compose.rules

import io.github.devvikassoni.foldfail.analyzer.compose.ComposeSourceParser
import io.github.devvikassoni.foldfail.core.Confidence
import io.github.devvikassoni.foldfail.core.FoldFailContext
import io.github.devvikassoni.foldfail.core.FoldFailFinding
import io.github.devvikassoni.foldfail.core.FoldFailRule
import io.github.devvikassoni.foldfail.core.Severity

class RestrictiveModifierChainsAnalyzer : FoldFailRule {

    override val id = "FF010"

    override val description =
        "Identify combinations of fixed-size modifiers that may constrain available space in resizable or larger-screen configurations"

    override fun analyze(context: FoldFailContext): List<FoldFailFinding> {
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
                            FoldFailFinding(
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
