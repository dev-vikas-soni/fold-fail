package com.viewlint.analyzer

import com.viewlint.analyzer.compose.rules.HardcodedDimensionAnalyzer
import com.viewlint.analyzer.compose.rules.MissingAdaptiveHandlingAnalyzer
import com.viewlint.analyzer.compose.rules.RequiredSizeAnalyzer
import com.viewlint.analyzer.compose.rules.RestrictiveModifierChainsAnalyzer
import com.viewlint.core.ViewlintRule

object ViewlintRuleRegistry {

    fun defaultRules(): List<ViewlintRule> = listOf(
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
