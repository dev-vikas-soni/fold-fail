package com.viewlint.analyzer

import com.viewlint.core.Confidence
import com.viewlint.core.ViewlintContext
import com.viewlint.core.ViewlintFinding
import com.viewlint.core.ViewlintRule
import com.viewlint.core.Severity

class MissingAdaptiveVariantsAnalyzer : ViewlintRule {

    override val id = "FF005"

    override val description =
        "Identifies complex layouts lacking alternative adaptive resource qualifiers"

    override fun analyze(context: ViewlintContext): List<ViewlintFinding> {
        if (context.layouts.isEmpty()) return emptyList()

        val qualifiersToCheck = listOf("sw600dp", "w600dp", "land", "sw720dp")
        val layoutsByName = context.layouts.groupBy { it.name }

        return buildList {
            val analyzedNames = mutableSetOf<String>()

            for (layout in context.layouts) {
                // Only analyze base "layout" or first encountered variant for each name
                if (!analyzedNames.add(layout.name)) continue

                val variants = layoutsByName[layout.name] ?: continue
                val hasAdaptiveVariant = variants.any { v ->
                    qualifiersToCheck.any { q -> v.qualifier.contains(q, ignoreCase = true) }
                }

                if (hasAdaptiveVariant) continue

                // Check node count complexity
                val parsed = LayoutParser.parse(layout.path.toFile())
                if (parsed.error != null || parsed.root == null) continue

                val nodeCount = countNodes(parsed.root)
                if (nodeCount > 15) {
                    add(
                        ViewlintFinding(
                            ruleId = id,
                            title = "Complex layout lacks adaptive variants",
                            message = "Layout '${layout.name}' has high structural complexity ($nodeCount nodes) but only a single layout variant.",
                            severity = Severity.INFO,
                            confidence = Confidence.LOW,
                            file = layout.path.toString(),
                            evidence = "Node count: $nodeCount, variants: ${variants.map { it.qualifier }}",
                            remediation = "Provide alternative layout qualifiers (e.g. layout-w600dp or layout-sw600dp) for tablets and foldables."
                        )
                    )
                }
            }
        }
    }

    private fun countNodes(node: LayoutNode): Int {
        return 1 + node.children.sumOf { countNodes(it) }
    }
}
