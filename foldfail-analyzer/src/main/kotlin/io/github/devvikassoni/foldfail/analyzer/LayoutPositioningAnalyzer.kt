package io.github.devvikassoni.foldfail.analyzer

import io.github.devvikassoni.foldfail.core.Confidence
import io.github.devvikassoni.foldfail.core.FoldFailContext
import io.github.devvikassoni.foldfail.core.FoldFailFinding
import io.github.devvikassoni.foldfail.core.FoldFailRule
import io.github.devvikassoni.foldfail.core.Severity

class LayoutPositioningAnalyzer : FoldFailRule {

    override val id = "FF006"

    override val description =
        "Identifies potentially fragile layout absolute or hardcoded positioning assumptions"

    override fun analyze(context: FoldFailContext): List<FoldFailFinding> {
        if (context.layouts.isEmpty()) return emptyList()

        return buildList {
            val analyzedFiles = mutableSetOf<String>()

            for (layout in context.layouts) {
                if (!analyzedFiles.add(layout.path.toString())) continue

                val parsed = LayoutParser.parse(layout.path.toFile())
                if (parsed.error != null || parsed.root == null) continue

                inspectNodeForPositioning(parsed.root, layout.path.toString(), this)
            }
        }
    }

    private fun inspectNodeForPositioning(node: LayoutNode, filePath: String, findings: MutableList<FoldFailFinding>) {
        if (node.tagName.equals("AbsoluteLayout", ignoreCase = true)) {
            findings.add(
                FoldFailFinding(
                    ruleId = id,
                    title = "AbsoluteLayout usage detected",
                    message = "Layout uses deprecated AbsoluteLayout which does not adapt to screen size changes.",
                    severity = Severity.WARNING,
                    confidence = Confidence.HIGH,
                    file = filePath,
                    evidence = "<AbsoluteLayout>",
                    remediation = "Replace AbsoluteLayout with ConstraintLayout, RelativeLayout, or FrameLayout with responsive constraints."
                )
            )
        } else if (node.attributes.containsKey("layout_x") || node.attributes.containsKey("layout_y")) {
            findings.add(
                FoldFailFinding(
                    ruleId = id,
                    title = "Hardcoded absolute coordinates",
                    message = "View <${node.tagName}> uses absolute layout_x/layout_y positioning attributes.",
                    severity = Severity.WARNING,
                    confidence = Confidence.MEDIUM,
                    file = filePath,
                    evidence = "layout_x=\"${node.attributes["layout_x"]}\" layout_y=\"${node.attributes["layout_y"]}\"",
                    remediation = "Use relative positioning constraints (e.g. ConstraintLayout) instead of hardcoded absolute screen coordinates."
                )
            )
        }

        for (child in node.children) {
            inspectNodeForPositioning(child, filePath, findings)
        }
    }
}
