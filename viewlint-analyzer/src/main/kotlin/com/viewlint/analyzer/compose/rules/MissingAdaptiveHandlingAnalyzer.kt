package com.viewlint.analyzer.compose.rules

import com.viewlint.analyzer.compose.ComposeSourceParser
import com.viewlint.core.Confidence
import com.viewlint.core.ViewlintContext
import com.viewlint.core.ViewlintFinding
import com.viewlint.core.ViewlintRule
import com.viewlint.core.Severity

class MissingAdaptiveHandlingAnalyzer : ViewlintRule {

    override val id = "FF009"

    override val description =
        "Identifies potentially complex screen-level composables that appear to use fixed layout assumptions without detectable adaptive branching"

    override fun analyze(context: ViewlintContext): List<ViewlintFinding> {
        if (context.kotlinSources.isEmpty()) return emptyList()

        return buildList {
            for (sourcePath in context.kotlinSources) {
                val parsed = ComposeSourceParser.parse(sourcePath.toFile())
                if (parsed.error != null) continue

                for (comp in parsed.composables) {
                    // Heuristic: If composable is large (text length > 300 chars) or has multiple hardcoded modifiers and no adaptive cues
                    val hasMultipleModifiers = comp.modifierCalls.size >= 2
                    val isLargeComposable = comp.text.length > 300

                    if ((hasMultipleModifiers || isLargeComposable) && !comp.hasAdaptiveCues) {
                        add(
                            ViewlintFinding(
                                ruleId = id,
                                title = "Missing adaptive layout handling",
                                message = "Composable '${comp.name}' is complex or uses multiple fixed modifiers without detectable adaptive branching (e.g. WindowSizeClass or BoxWithConstraints).",
                                severity = Severity.INFO,
                                confidence = Confidence.LOW,
                                file = sourcePath.toString(),
                                evidence = "Composable: ${comp.name} (${comp.modifierCalls.size} fixed modifiers)",
                                remediation = "Consider incorporating WindowSizeClass, BoxWithConstraints, or responsive layout branching to provide optimal experiences across phones, foldables, and tablets."
                            )
                        )
                    }
                }
            }
        }
    }
}
