package com.viewlint.analyzer.compose.rules

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
                    if (comp.isInputSurface) continue
                    if (comp.isLeafOrSmallComponent && !comp.hasTextContent) continue

                    // Internal scrolling mitigates fixed container height/width restriction
                    if (comp.hasInternalScroll) continue

                    // Check if both width and height are fixed and will be reported by FF010
                    val fixedWidthCall = comp.modifierCalls.find { it.contains("width(") && it.contains("dp") && !it.contains("widthIn") && !it.contains("required") }
                    val fixedHeightCall = comp.modifierCalls.find { it.contains("height(") && it.contains("dp") && !it.contains("heightIn") && !it.contains("required") }
                    val fixedSizeCall = comp.modifierCalls.find { it.contains("size(") && it.contains("dp") && !it.contains("sizeIn") && !it.contains("required") }

                    val isLockedBothAxes = (fixedWidthCall != null && fixedHeightCall != null) || fixedSizeCall != null
                    if (isLockedBothAxes) {
                        // Suppress FF007 in favor of FF010 to avoid redundant duplicate reporting on the exact same composable
                        continue
                    }

                    val reportedCallsInComposable = mutableSetOf<String>()

                    for (call in comp.modifierCalls) {
                        if (!call.contains("dp") || call.contains("required")) continue

                        // Check dimension modifiers: width, height, size
                        val isDimensionCall = call.startsWith("width(") || call.startsWith("height(") || call.startsWith("size(")
                        if (!isDimensionCall) continue

                        // Do not treat flexible range modifiers (widthIn / heightIn / sizeIn) as rigid fixed dimensions
                        if (call.contains("widthIn") || call.contains("heightIn") || call.contains("sizeIn")) continue

                        val dpValue = parseDpValue(call) ?: continue

                        // Exclude touch targets, icons, badges, spacers (<= 48dp)
                        if (dpValue <= 48.0) continue

                        // Exclude small visual elements (<= 64dp) without dynamic text
                        if (dpValue <= 64.0 && !comp.hasTextContent) continue

                        // Deduplicate identical call line within the same composable
                        if (!reportedCallsInComposable.add(call)) continue

                        val dimenType = when {
                            call.contains("height") -> "height"
                            call.contains("width") -> "width"
                            else -> "dimension"
                        }

                        val confidence = when {
                            dpValue > 120.0 && comp.isScreenOrContainer -> Confidence.HIGH
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
                                remediation = "Review whether '${comp.name}' needs to expand across foldables, tablets, or split-screen windows. Consider using flexible modifiers (fillMaxWidth, weight) or adapting to WindowSizeClass / BoxWithConstraints."
                            )
                        )
                    }
                }
            }
        }
    }

    private fun parseDpValue(call: String): Double? {
        val match = Regex("(?:width|height|size)\\s*\\(\\s*([0-9]+(?:\\.[0-9]+)?)\\s*\\.\\s*dp").find(call)
            ?: Regex("(?:width|height|size|max)\\s*=\\s*([0-9]+(?:\\.[0-9]+)?)\\s*\\.\\s*dp").find(call)
            ?: Regex("([0-9]+(?:\\.[0-9]+)?)\\s*\\.\\s*dp").find(call)
        return match?.groupValues?.get(1)?.toDoubleOrNull()
    }
}
