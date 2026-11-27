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

                    val reportedCallsInComposable = mutableSetOf<String>()

                    for (call in comp.modifierCalls) {
                        if (!call.contains("dp") || call.contains("required")) continue

                        // Check dimension modifiers: width, height, size, widthIn, heightIn
                        val isDimensionCall = call.contains("width") || call.contains("height") || call.contains("size")
                        if (!isDimensionCall) continue

                        // Ignore minimum bounds (e.g. widthIn(min = 48.dp) or heightIn(min = 56.dp))
                        if (call.contains("min") && !call.contains("max")) continue

                        val dpValue = parseDpValue(call) ?: continue

                        // Thresholds & Role Exclusions:
                        // - Dimensions <= 48dp are standard touch targets, icons, avatars, badges, or spacers.
                        // - Dimensions <= 64dp on leaf / small components are standard visual design specs.
                        if (dpValue <= 48.0) continue
                        if (dpValue <= 64.0 && comp.isLeafOrSmallComponent) continue

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
        val match = Regex("(?:width|height|size|widthIn|heightIn)\\s*\\(\\s*([0-9]+(?:\\.[0-9]+)?)\\s*\\.\\s*dp").find(call)
            ?: Regex("(?:width|height|size|max)\\s*=\\s*([0-9]+(?:\\.[0-9]+)?)\\s*\\.\\s*dp").find(call)
            ?: Regex("([0-9]+(?:\\.[0-9]+)?)\\s*\\.\\s*dp").find(call)
        return match?.groupValues?.get(1)?.toDoubleOrNull()
    }
}
