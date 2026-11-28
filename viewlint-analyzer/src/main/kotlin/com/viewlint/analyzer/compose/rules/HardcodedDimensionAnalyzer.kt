package com.viewlint.analyzer.compose.rules

import com.viewlint.analyzer.compose.ComponentRole
import com.viewlint.analyzer.compose.ComposeSourceParser
import com.viewlint.core.Confidence
import com.viewlint.core.Severity
import com.viewlint.core.ViewlintContext
import com.viewlint.core.ViewlintFinding
import com.viewlint.core.ViewlintRule

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

                    // Do not flag input surfaces (e.g. OTP text field cells) or pure leaf graphics without variable text
                    if (comp.role == ComponentRole.INPUT_SURFACE) continue
                    if (comp.role == ComponentRole.LEAF_OR_SMALL && !comp.hasTextContent) continue

                    val reportedCallsInComposable = mutableSetOf<String>()

                    for (chain in comp.modifierChains) {
                        // Check if internal scroll applies directly to THIS modifier chain / container
                        val isChainScrollMitigated = chain.hasScrollModifier || isInherentlyScrollablePrimitive(chain.targetPrimitive)
                        if (isChainScrollMitigated) continue

                        // Check if both width and height are fixed in this unscrollable chain and will be reported by FF010
                        val fixedWidthCall = chain.calls.find { it.contains("width(") && it.contains("dp") && !it.contains("widthIn") && !it.contains("required") }
                        val fixedHeightCall = chain.calls.find { it.contains("height(") && it.contains("dp") && !it.contains("heightIn") && !it.contains("required") }
                        val fixedSizeCall = chain.calls.find { it.contains("size(") && it.contains("dp") && !it.contains("sizeIn") && !it.contains("required") }

                        val isLockedBothAxes = (fixedWidthCall != null && fixedHeightCall != null) || fixedSizeCall != null
                        if (isLockedBothAxes) {
                            // Suppress FF007 in favor of FF010 to avoid redundant duplicate reporting
                            continue
                        }

                        for (call in chain.calls) {
                            if (!call.contains("dp") || call.contains("required")) continue

                            val isDimensionCall = call.startsWith("width(") || call.startsWith("height(") || call.startsWith("size(")
                            if (!isDimensionCall) continue

                            if (call.contains("widthIn") || call.contains("heightIn") || call.contains("sizeIn")) continue

                            val dpValue = parseDpValue(call) ?: continue

                            // Exclude touch targets, icons, badges, spacers (<= 48dp)
                            if (dpValue <= 48.0) continue

                            // Exclude small visual elements (<= 64dp) without dynamic text
                            if (dpValue <= 64.0 && !comp.hasTextContent) continue

                            if (!reportedCallsInComposable.add(call)) continue

                            val dimenType = when {
                                call.contains("height") -> "height"
                                call.contains("width") -> "width"
                                else -> "dimension"
                            }

                            val confidence = when {
                                comp.role == ComponentRole.SCREEN_OR_CONTAINER && dpValue > 120.0 -> Confidence.HIGH
                                comp.role == ComponentRole.UNKNOWN -> Confidence.MEDIUM
                                dpValue > 120.0 -> Confidence.HIGH
                                dpValue > 64.0 -> Confidence.MEDIUM
                                else -> Confidence.LOW
                            }

                            val message = "Composable '${comp.name}' uses a fixed ${dpValue.toInt()}dp $dimenType ('$call') on a layout region without detectable adaptive handling."

                            add(
                                ViewlintFinding(
                                    ruleId = id,
                                    title = "Composable uses hardcoded dimensions",
                                    message = message,
                                    severity = Severity.WARNING,
                                    confidence = confidence,
                                    file = sourcePath.toString(),
                                    evidence = call,
                                    remediation = "Review whether '${comp.name}' needs to expand across foldables, tablets, or split-screen windows. Consider using flexible modifiers (fillMaxWidth, weight) or adapting to WindowSizeClass / BoxWithConstraints.",
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

    private fun parseDpValue(call: String): Double? {
        val match = Regex("(?:width|height|size)\\s*\\(\\s*([0-9]+(?:\\.[0-9]+)?)\\s*\\.\\s*dp").find(call)
            ?: Regex("(?:width|height|size|max)\\s*=\\s*([0-9]+(?:\\.[0-9]+)?)\\s*\\.\\s*dp").find(call)
            ?: Regex("([0-9]+(?:\\.[0-9]+)?)\\s*\\.\\s*dp").find(call)
        return match?.groupValues?.get(1)?.toDoubleOrNull()
    }
}
