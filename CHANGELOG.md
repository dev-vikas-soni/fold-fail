# ViewLint Changelog

All notable changes to ViewLint will be documented in this file.

## [1.0.0] — Production Release — Stable Android Adaptive Quality Auditor

### Highlights
- **First Stable Production Launch**: ViewLint 1.0.0 is certified for production deployment across Android Gradle builds, CI/CD pipelines, and multi-module repositories.
- **Stable Public API Freeze**: Guaranteed API stability for:
  - Gradle plugin extension `viewlint { enabled, runOnCheck, reportFormats, outputDir, failOn, baselineFile }`
  - Gradle task `viewlint` (type `ViewlintTask`)
  - Report formats: Console, JSON (`viewlint-report.json`), HTML (`viewlint-report.html`), and OASIS SARIF 2.1.0 (`viewlint-report.sarif`).
  - Baseline format and quality gate execution logic.
- **FF007 Semantic Soundness Unification**:
  - Clarified that scrollable ancestors (`LazyColumn`) do not automatically make unscrollable fixed-height child cards safe from text clipping.
  - Mitigated fixed dimension constraints when internal scroll applies directly to the constrained modifier chain (`.verticalScroll` or `LazyColumn`).
  - Preserved analysis eligibility for content-bearing cards, sheets, and forms.
  - Preserved `ComponentRole.UNKNOWN` when component role keywords do not match, evaluating with `Confidence.MEDIUM`.
- **45-Fixture Evidence-Driven Benchmark Suite**:
  - Reconciled all 45 test fixtures in `AccuracyBenchmarkTest.kt` ($19\text{ TP} + 26\text{ TN} = 45\text{ Total Fixtures}$).
  - Asserts exact Rule ID, Severity, Confidence, Evidence substring, and Line number for every fixture.
  - Fails build if unpermitted rules trigger on a test fixture.
- **Performance & Overhead Discrepancy Breakdown**:
  - Resolved timing discrepancies by separating total Gradle CLI process invocation overhead (warm p50 ~930 ms) from direct in-memory engine analysis duration (`ViewlintEngine.analyze()`, ~20–80 ms across 45 fixtures).
- **Comprehensive Documentation Architecture**:
  - Reorganized project documentation into structured directories: `docs/getting-started/`, `docs/rules/` (independent FF001–FF010 rule guides), `docs/guides/`, `docs/engineering/`, and `docs/contributing/`.

### Installation

```kotlin
plugins {
    id("io.github.dev-vikas-soni.viewlint") version "1.0.0"
}
```

---

## [0.9.0] — Modern Light Theme Report & Priority Engine Upgrade

### Highlights
- **Modern Light Theme HTML Report**: Complete light theme redesign with crisp Slate typography (`#0F172A`), soft off-white canvas (`#F8FAFC`), white card surfaces, soft pastel badges for severity, confidence, and priority, sticky interactive filter/search controls bar, live matching count indicator, and Top Adaptive Risks dashboard.
- **Deterministic Priority Model**: Added `Priority` (`CRITICAL`, `HIGH`, `MEDIUM`, `LOW`) computed deterministically from `Severity` and `Confidence` across all finding records and report formats (Console, JSON, HTML, SARIF).
- **Rule Precision Refinements**:
  - `FF007`: Recognized `widthIn(max = ...)` / `heightIn(max = ...)` max-width range constraints as responsive design patterns rather than hardcoded dimensions. Extended visual component exclusions to progress indicators, switches, checkboxes, ratings, images, and thumbnails.
  - `FF008`: Calibrated `requiredSize` severity/confidence depending on whether used on content containers (`WARNING` / `HIGH` confidence) vs small visual elements (`INFO` / `LOW` confidence).
  - `FF009`: Restricted adaptive responsibility analysis to top-level screen composables, scaffolds, and page containers, ignoring reusable item cards and helper functions.
  - `FF010`: Excluded small visual icons/buttons from dual-axis lock warnings.
- **Single Authoritative Version Source**: Unified version string across all metadata structures, report outputs, plugin descriptors, and documentation.

---

## [0.8.0] — Analyzer Precision & Report Intelligence Upgrade

### Highlights
- **Context-Aware Compose Dimension Analysis (FF007)**: Added numeric `dp` value parsing, component-role awareness (excluding standard touch targets <= 48dp, icons, avatars, badges, chips, dividers, spacers), range modifier handling (widthIn/heightIn min bounds), and component-specific contextual messages.
- **Top Adaptive Risks Dashboard**: Redesigned HTML report header featuring a prioritized Top Adaptive Risks dashboard ranking critical findings by deterministic risk scores.
- **Screen & File Level Risk Summaries**: Added screen/file risk breakdown tables displaying risk counts, primary severities, and primary rule IDs per file.
- **Confidence Rationale & Deterministic Calibration**: Formalized Confidence model (HIGH, MEDIUM, LOW) with human-readable rationale in finding cards and report outputs.

---

## [0.7.1] — Official First Public Release

### Highlights
- **First Public Viewlint Release**: Official public launch of Viewlint (`io.github.dev-vikas-soni.viewlint` Gradle plugin ID, `com.viewlint` package namespace, `viewlint` extension and task).
- **Ten Adaptive UI Static-Analysis Rules**: Full support for rules FF001–FF010 auditing Android Manifests, XML Layout resources, and Jetpack Compose `@Composable` functions.
- **Gradle Lifecycle Integration**: Automatic wiring into `./gradlew check` standard verification lifecycle with configurable quality gates (`NEVER`, `ERROR`, `WARNING`, `NEW`).
- **Multi-Format Reporting**: Export diagnostic reports in Console, JSON (`viewlint-report.json`), HTML (`viewlint-report.html`), and SARIF 2.1.0 (`viewlint-report.sarif`).
- **Baselines & Quality Control**: Baseline signature generation and suppression (`viewlint-baseline.txt`) for managing legacy technical debt.
