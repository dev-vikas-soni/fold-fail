# Semantic Analysis & Heuristic Decision Models

## Compose Source Parser (`ComposeSourceParser`)
ViewLint parses Kotlin source code using a fast, zero-dependency sanitizer and tokenizer that:
1. Strips single-line and multi-line comments and string literal contents to prevent false keyword matches.
2. Identifies `@Composable fun Name()` signatures with exact line numbers.
3. Classifies composables into component roles (`SCREEN_OR_CONTAINER`, `LEAF_OR_SMALL`, `INPUT_SURFACE`, or `UNKNOWN`).
4. Extracts modifier chains (`Modifier.width(...).height(...)`).
5. Detects adaptive cues (`WindowSizeClass`, `BoxWithConstraints`, `NavigationRail`, `FlowRow`, etc.).

## Semantic Soundness Principles
- **No Blanket Exemptions**: Parent containers like `LazyColumn` do not automatically make unscrollable fixed-height child cards safe.
- **Internal Scroll Mitigation**: Modifier chains with explicit `.verticalScroll(...)` or `LazyColumn` targets are mitigated.
- **Deduplication**: When `FF010` triggers on a chain locked in both axes, `FF007` is suppressed to avoid duplicate findings.
