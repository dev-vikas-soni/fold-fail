package com.viewlint.analyzer

import com.viewlint.core.Confidence
import com.viewlint.core.ViewlintContext
import com.viewlint.core.ViewlintFinding
import com.viewlint.core.ViewlintRule
import com.viewlint.core.Severity

class LayoutPositioningAnalyzer : ViewlintRule {

    override val id = "FF006"

    override val description =
        "Identifies potentially fragile layout absolute or hardcoded positioning assumptions"

    override fun analyze(context: ViewlintContext): List<ViewlintFinding> {
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

    private fun inspectNodeForPositioning(node: LayoutNode, filePath: String, findings: MutableList<ViewlintFinding>) {
        if (node.tagName.equals("AbsoluteLayout", ignoreCase = true)) {
            findings.add(
                ViewlintFinding(
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
        } else if (node.attributes.keys.any { it.endsWith("layout_x") || it.endsWith("layout_y") }) {
            findings.add(
                ViewlintFinding(
                    ruleId = id,
                    title = "Hardcoded absolute layout coordinates",
                    message = "View <${node.tagName}> specifies layout_x or layout_y which causes rigid absolute placement.",
                    severity = Severity.WARNING,
                    confidence = Confidence.MEDIUM,
                    file = filePath,
                    evidence = "layout_x / layout_y attributes",
                    remediation = "Use relative constraints or guideline positioning instead of fixed coordinate attributes."
                )
            )
        }

        for (child in node.children) {
            inspectNodeForPositioning(child, filePath, findings)
        }
    }
}
