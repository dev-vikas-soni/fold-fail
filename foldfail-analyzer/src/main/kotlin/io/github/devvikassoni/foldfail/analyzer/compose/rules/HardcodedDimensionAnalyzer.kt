package io.github.devvikassoni.foldfail.analyzer.compose.rules

import io.github.devvikassoni.foldfail.analyzer.compose.ComposeSourceParser
import io.github.devvikassoni.foldfail.core.Confidence
import io.github.devvikassoni.foldfail.core.FoldFailContext
import io.github.devvikassoni.foldfail.core.FoldFailFinding
import io.github.devvikassoni.foldfail.core.FoldFailRule
import io.github.devvikassoni.foldfail.core.Severity

class HardcodedDimensionAnalyzer : FoldFailRule {

    override val id = "FF007"

    override val description =
        "Detects potentially restrictive hardcoded dimensions in Compose modifier chains"

    override fun analyze(context: FoldFailContext): List<FoldFailFinding> {
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
                                FoldFailFinding(
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
