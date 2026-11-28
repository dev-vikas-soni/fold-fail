package com.viewlint.analyzer.compose.rules

import com.viewlint.analyzer.compose.ComponentRole
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

                    if (comp.role == ComponentRole.INPUT_SURFACE) continue
                    if (comp.role == ComponentRole.LEAF_OR_SMALL && !comp.hasTextContent) continue

                    for (chain in comp.modifierChains) {
                        val isChainScrollMitigated = chain.hasScrollModifier || isInherentlyScrollablePrimitive(chain.targetPrimitive)
                        if (isChainScrollMitigated) continue

                        val fixedWidthCall = chain.calls.find { it.contains("width(") && it.contains("dp") && !it.contains("widthIn") && !it.contains("required") }
                        val fixedHeightCall = chain.calls.find { it.contains("height(") && it.contains("dp") && !it.contains("heightIn") && !it.contains("required") }
                        val fixedSizeCall = chain.calls.find { it.contains("size(") && it.contains("dp") && !it.contains("sizeIn") && !it.contains("required") }

                        val isLockedBothAxes = (fixedWidthCall != null && fixedHeightCall != null) || fixedSizeCall != null

                        if (isLockedBothAxes) {
                            val evidence = listOfNotNull(fixedWidthCall, fixedHeightCall, fixedSizeCall).joinToString(", ")
                            val confidence = when {
                                comp.role == ComponentRole.SCREEN_OR_CONTAINER -> Confidence.HIGH
                                comp.role == ComponentRole.UNKNOWN -> Confidence.MEDIUM
                                else -> Confidence.LOW
                            }

                            add(
                                ViewlintFinding(
                                    ruleId = id,
                                    title = "Restrictive modifier chain combination",
                                    message = "Composable '${comp.name}' combines fixed width and height or fixed size modifiers ($evidence), locking component boundaries in both horizontal and vertical axes.",
                                    severity = Severity.WARNING,
                                    confidence = confidence,
                                    file = sourcePath.toString(),
                                    evidence = evidence,
                                    remediation = "Avoid locking both width and height to fixed dp values on content containers. Use weight, fillMaxSize, or responsive sizing constraints.",
                                    line = comp.line
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    private fun isInherentlyScrollablePrimitive(primitive: String?): Boolean {
        if (primitive == null) return false
        val p = primitive.lowercase()
        return p == "lazycolumn" || p == "lazyrow" || p == "lazyverticalgrid" || p == "lazyhorizontalgrid"
    }
}
