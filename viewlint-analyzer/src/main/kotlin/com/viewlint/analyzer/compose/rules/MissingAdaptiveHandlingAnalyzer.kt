package com.viewlint.analyzer.compose.rules

import com.viewlint.analyzer.compose.ComposeSourceParser
import com.viewlint.core.Confidence
import com.viewlint.core.Severity
import com.viewlint.core.ViewlintContext
import com.viewlint.core.ViewlintFinding
import com.viewlint.core.ViewlintRule

class MissingAdaptiveHandlingAnalyzer : ViewlintRule {

    override val id = "FF009"

    override val description =
        "Identifies screen-level composables or major content containers that lack responsive layout handling for larger or fold-state window configurations"

    override fun analyze(context: ViewlintContext): List<ViewlintFinding> {
        if (context.kotlinSources.isEmpty()) return emptyList()

        return buildList {
            for (sourcePath in context.kotlinSources) {
                val parsed = ComposeSourceParser.parse(sourcePath.toFile())
                if (parsed.error != null) continue

                for (comp in parsed.composables) {
                    // Do not analyze leaf composables, small controls, quantity selectors, or decorative elements
                    if (comp.isLeafOrSmallComponent) continue

                    // Only target genuine screen-level composables, forms, dashboards, or major content containers
                    if (!comp.isScreenOrContainer) continue

                    if (comp.hasAdaptiveCues) continue

                    val hasMultipleModifiers = comp.modifierCalls.size >= 2
                    val isSubstantialContainer = comp.text.length > 250

                    if (hasMultipleModifiers || isSubstantialContainer) {
                        val remediation = when {
                            comp.text.lowercase().contains("lazyverticalgrid") || comp.text.lowercase().contains("grid") ->
                                "Consider using GridCells.Adaptive or FlowRow to automatically adjust column counts across phone, foldable, and tablet displays."
                            comp.name.lowercase().contains("screen") || comp.name.lowercase().contains("dashboard") ->
                                "Incorporate WindowSizeClass or calculateWindowSizeClass() to switch between single-pane and multi-pane (or NavigationRail) layouts on expanded windows."
                            else ->
                                "Incorporate WindowSizeClass, BoxWithConstraints, or flexible weights (fillMaxWidth) to allow content regions to adapt across variable window dimensions."
                        }

                        add(
                            ViewlintFinding(
                                ruleId = id,
                                title = "Missing adaptive layout handling",
                                message = "Screen container '${comp.name}' defines multi-region or constrained layout structures without detectable adaptive branching (e.g. WindowSizeClass or BoxWithConstraints).",
                                severity = Severity.INFO,
                                confidence = Confidence.LOW,
                                file = sourcePath.toString(),
                                evidence = "Composable: ${comp.name} (${comp.modifierCalls.size} modifiers, ${comp.text.length} chars)",
                                remediation = remediation
                            )
                        )
                    }
                }
            }
        }
    }
}
