# Viewlint Limitations & Known Constraints

While Viewlint provides robust static analysis for Android adaptive quality, maintainers and engineering teams should be aware of the following technical boundaries:

1. **Static Analysis vs. Runtime Behavior**: Viewlint inspects declarations and source text statically. It cannot prove runtime behavior, custom view measurement logic, dynamic state flows, or runtime window sizes.
2. **Heuristic-Based Compose Analysis**: Compose source parsing uses structural pattern matching and keyword heuristics (`WindowSizeClass`, `BoxWithConstraints`, etc.). Complex state-driven adaptive logic or custom modifiers may occasionally produce false positives or false negatives.
3. **Intentional Fixed Dimensions**: Certain UI elements (e.g., standard launcher icons, fixed touch targets, or splash screens) may intentionally use fixed dimensions. Quality gates and baselines should be used to triage and suppress known intentional occurrences.
4. **Qualified Resource Variants**: Presence of a qualified resource variant (e.g., `layout-sw600dp`) indicates consideration of adaptive design but does not guarantee correctness or complete rendering fidelity across all screen dimensions.
5. **SARIF Ingestion & CI**: SARIF reports normalize absolute paths to repository-relative URIs. External verification of GitHub Code Scanning ingestion depends on repository visibility and security event permissions.
