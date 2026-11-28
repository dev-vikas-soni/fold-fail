package com.viewlint.analyzer.model

import java.nio.file.Path

enum class ComponentRole {
    LEAF_CONTROL,
    DECORATIVE_ELEMENT,
    INPUT_SURFACE,
    CONTENT_CONTAINER,
    SCREEN_LAYOUT,
    NAVIGATION_COMPONENT,
    UNKNOWN
}

enum class ConstraintType {
    EXACT_DP,
    REQUIRED_EXACT_DP,
    RANGE_DP,
    FLEXIBLE_FILL,
    FLEXIBLE_WRAP,
    WEIGHT,
    UNCONSTRAINED
}

data class DimensionConstraint(
    val type: ConstraintType,
    val valueDp: Double? = null,
    val minDp: Double? = null,
    val maxDp: Double? = null,
    val isRequired: Boolean = false,
    val rawExpression: String = ""
)

enum class SemanticConfidence {
    HIGH,
    MEDIUM,
    LOW
}

data class SemanticUiNode(
    val id: String,
    val name: String,
    val sourceFile: Path,
    val startLine: Int = 1,
    val role: ComponentRole = ComponentRole.UNKNOWN,
    val widthConstraint: DimensionConstraint = DimensionConstraint(ConstraintType.UNCONSTRAINED),
    val heightConstraint: DimensionConstraint = DimensionConstraint(ConstraintType.UNCONSTRAINED),
    val hasScrollableParent: Boolean = false,
    val isScrollContainer: Boolean = false,
    val hasAdaptiveEvidence: Boolean = false,
    val adaptiveEvidenceDetails: List<String> = emptyList(),
    val rawModifierChain: List<String> = emptyList(),
    val children: List<SemanticUiNode> = emptyList(),
    val confidence: SemanticConfidence = SemanticConfidence.HIGH
)
