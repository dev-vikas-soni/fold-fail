package io.github.devvikassoni.foldfail.analyzer

import io.github.devvikassoni.foldfail.core.Confidence
import io.github.devvikassoni.foldfail.core.FoldFailContext
import io.github.devvikassoni.foldfail.core.FoldFailFinding
import io.github.devvikassoni.foldfail.core.FoldFailRule
import io.github.devvikassoni.foldfail.core.Severity

class FixedDimensionAnalyzer : FoldFailRule {

    override val id = "FF004"

    override val description =
        "Detects potentially restrictive fixed inner dimensions in layout hierarchies"

    override fun analyze(context: FoldFailContext): List<FoldFailFinding> {
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

    private fun inspectNodeRecursive(node: LayoutNode, filePath: String, findings: MutableList<FoldFailFinding>) {
        val width = node.width
        val height = node.height

        if (width != null && height != null && !node.isScrollContainer) {
            val widthPx = parseDp(width)
            val heightPx = parseDp(height)

            // Flag if both width and height are large fixed dp values (e.g., >= 300dp)
            if (widthPx != null && heightPx != null && widthPx >= 300 && heightPx >= 300) {
                findings.add(
                    FoldFailFinding(
                        ruleId = id,
                        title = "Fixed large inner dimensions",
                        message = "View <${node.tagName}> specifies fixed large layout_width=\"$width\" and layout_height=\"$height\" on a non-scroll container.",
                        severity = Severity.INFO,
                        confidence = Confidence.LOW,
                        file = filePath,
                        evidence = "layout_width=\"$width\" layout_height=\"$height\"",
                        remediation = "Ensure large containers use match_parent or flexible constraints to adapt to wider screen configurations rather than fixed large dp bounds."
                    )
                )
            }
        }

        for (child in node.children) {
            inspectNodeRecursive(child, filePath, findings)
        }
    }

    private fun parseDp(dimension: String): Int? {
        val clean = dimension.trim()
        if (!clean.endsWith("dp", ignoreCase = true)) return null
        val numStr = clean.substringBefore("dp").substringBefore("DP").trim()
        return numStr.toFloatOrNull()?.toInt()
    }
}
