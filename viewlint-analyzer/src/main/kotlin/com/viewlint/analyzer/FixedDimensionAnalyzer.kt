package com.viewlint.analyzer

import com.viewlint.core.Confidence
import com.viewlint.core.ViewlintContext
import com.viewlint.core.ViewlintFinding
import com.viewlint.core.ViewlintRule
import com.viewlint.core.Severity

class FixedDimensionAnalyzer : ViewlintRule {

    override val id = "FF004"

    override val description =
        "Detects potentially restrictive fixed inner dimensions in layout hierarchies"

    override fun analyze(context: ViewlintContext): List<ViewlintFinding> {
        if (context.layouts.isEmpty()) return emptyList()

        return buildList {
            val analyzedFiles = mutableSetOf<String>()

            for (layout in context.layouts) {
                if (!analyzedFiles.add(layout.path.toString())) continue

                val parsed = LayoutParser.parse(layout.path.toFile())
                if (parsed.error != null || parsed.root == null) continue

                // Traverse hierarchy to find restrictive fixed dimensions on non-scroll elements
                inspectNodeRecursive(parsed.root, layout.path.toString(), this)
            }
        }
    }

    private fun inspectNodeRecursive(node: LayoutNode, filePath: String, findings: MutableList<ViewlintFinding>) {
        val width = node.width
        val height = node.height

        if (width != null && height != null && !node.isScrollContainer) {
            val widthPx = parseDp(width)
            val heightPx = parseDp(height)

            // Flag if both width and height are large fixed dp values (e.g., >= 300dp)
            if (widthPx != null && heightPx != null && widthPx >= 300 && heightPx >= 300) {
                findings.add(
                    ViewlintFinding(
                        ruleId = id,
                        title = "Fixed large inner dimensions",
                        message = "View <${node.tagName}> specifies fixed large layout_width=\"$width\" and layout_height=\"$height\" on a non-scroll container.",
                        severity = Severity.INFO,
                        confidence = Confidence.LOW,
                        file = filePath,
                        evidence = "layout_width=\"$width\", layout_height=\"$height\"",
                        remediation = "Use flexible constraints, percentage widths, or wrap_content/match_parent to adapt to variable window sizes."
                    )
                )
            }
        }

        for (child in node.children) {
            inspectNodeRecursive(child, filePath, findings)
        }
    }

    private fun parseDp(dimen: String): Int? {
        val trimmed = dimen.trim().lowercase()
        if (trimmed.endsWith("dp")) {
            return trimmed.removeSuffix("dp").toFloatOrNull()?.toInt()
        }
        return null
    }
}
