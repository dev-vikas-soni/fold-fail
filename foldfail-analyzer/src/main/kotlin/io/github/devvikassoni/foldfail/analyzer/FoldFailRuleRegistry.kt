package io.github.devvikassoni.foldfail.analyzer

import io.github.devvikassoni.foldfail.analyzer.compose.rules.HardcodedDimensionAnalyzer
import io.github.devvikassoni.foldfail.analyzer.compose.rules.MissingAdaptiveHandlingAnalyzer
import io.github.devvikassoni.foldfail.analyzer.compose.rules.RequiredSizeAnalyzer
import io.github.devvikassoni.foldfail.analyzer.compose.rules.RestrictiveModifierChainsAnalyzer
import io.github.devvikassoni.foldfail.core.FoldFailRule

object FoldFailRuleRegistry {

    fun defaultRules(): List<FoldFailRule> = listOf(
        OrientationRestrictionAnalyzer(),
        AspectRatioAnalyzer(),
        FixedRootWidthAnalyzer(),
        FixedDimensionAnalyzer(),
        MissingAdaptiveVariantsAnalyzer(),
        LayoutPositioningAnalyzer(),
        HardcodedDimensionAnalyzer(),
        RequiredSizeAnalyzer(),
        MissingAdaptiveHandlingAnalyzer(),
        RestrictiveModifierChainsAnalyzer()
    )
}
