package io.github.devvikassoni.foldfail.analyzer

import io.github.devvikassoni.foldfail.core.Confidence
import io.github.devvikassoni.foldfail.core.FoldFailContext
import io.github.devvikassoni.foldfail.core.FoldFailFinding
import io.github.devvikassoni.foldfail.core.FoldFailRule
import io.github.devvikassoni.foldfail.core.Severity

class FixedRootWidthAnalyzer : FoldFailRule {

    override val id = "FF003"

    override val description =
        "Detects potentially restrictive fixed-width root layout configurations"

    override fun analyze(context: FoldFailContext): List<FoldFailFinding> {
        if (context.layouts.isEmpty()) return emptyList()

        val qualifiersToCheck = listOf("sw600dp", "w600dp", "land", "sw720dp")
        val layoutsByName = context.layouts.groupBy { it.name }

        return buildList {
            val analyzedFiles = mutableSetOf<String>()

            for (layout in context.layouts) {
                if (!analyzedFiles.add(layout.path.toString())) continue

                val parsed = LayoutParser.parse(layout.path.toFile())
                if (parsed.error != null || parsed.root == null) continue

                val root = parsed.root
                val width = root.width ?: continue

                val isHardcodedDp = width.matches(Regex("^\\d+(\\.\\d+)?dp$"))
                if (!isHardcodedDp) continue

                val hasAlternatives = layoutsByName[layout.name]?.any { other ->
                    qualifiersToCheck.any { q -> other.qualifier.contains(q, ignoreCase = true) }
                } == true

                if (hasAlternatives) continue

                add(
                    FoldFailFinding(
                        ruleId = id,
                        title = "Root layout has fixed width",
                        message = "Root element <${root.tagName}> in ${layout.path.fileName} specifies fixed layout_width=\"$width\" without responsive adaptive variants.",
                        severity = Severity.WARNING,
                        confidence = Confidence.MEDIUM,
                        file = layout.path.toString(),
                        evidence = "layout_width=\"$width\"",
                        remediation = "Use match_parent, wrap_content, 0dp (match constraints), or provide adaptive layout qualifiers (e.g., sw600dp) for tablets and foldables."
                    )
                )
            }
        }
    }
}
