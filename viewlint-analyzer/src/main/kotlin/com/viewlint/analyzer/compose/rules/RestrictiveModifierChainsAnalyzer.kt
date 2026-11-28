package com.viewlint.analyzer.compose.rules

import com.viewlint.analyzer.compose.ComposeSourceParser
import com.viewlint.core.Confidence
import com.viewlint.core.Severity
import com.viewlint.core.ViewlintContext
import com.viewlint.core.ViewlintFinding
import com.viewlint.core.ViewlintRule

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

                    // Ignore input surfaces (OTP text field cells, text inputs) and leaf graphics without dynamic text
                    if (comp.isInputSurface) continue
                    if (comp.isLeafOrSmallComponent && !comp.hasTextContent) continue

                    // Ignore items with internal scroll state as internal content can scroll
                    if (comp.hasInternalScroll) continue

                    val fixedWidthCall = comp.modifierCalls.find { it.contains("width(") && it.contains("dp") && !it.contains("widthIn") && !it.contains("required") }
                    val fixedHeightCall = comp.modifierCalls.find { it.contains("height(") && it.contains("dp") && !it.contains("heightIn") && !it.contains("required") }
                    val fixedSizeCall = comp.modifierCalls.find { it.contains("size(") && it.contains("dp") && !it.contains("sizeIn") && !it.contains("required") }

                    val isLockedBothAxes = (fixedWidthCall != null && fixedHeightCall != null) || fixedSizeCall != null

                    if (isLockedBothAxes) {
                        val evidence = listOfNotNull(fixedWidthCall, fixedHeightCall, fixedSizeCall).joinToString(", ")
                        val confidence = if (comp.isScreenOrContainer) Confidence.HIGH else Confidence.MEDIUM

                        add(
                            ViewlintFinding(
                                ruleId = id,
                                title = "Restrictive modifier chain combination",
                                message = "Composable '${comp.name}' combines fixed width and height or fixed size modifiers ($evidence), locking component boundaries in both horizontal and vertical axes.",
                                severity = Severity.WARNING,
                                confidence = confidence,
                                file = sourcePath.toString(),
                                evidence = evidence,
                                remediation = "Avoid locking both width and height to fixed dp values on content containers. Use weight, fillMaxSize, or responsive sizing constraints."
                            )
                        )
                    }
                }
            }
        }
    }
}
