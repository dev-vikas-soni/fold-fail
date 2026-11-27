# Viewlint Changelog

All notable changes to Viewlint will be documented in this file.

## [0.7.1] — Official First Public Release

### Highlights
- **First Public Viewlint Release**: Official public launch of Viewlint (`io.github.dev-vikas-soni.viewlint` Gradle plugin ID, `com.viewlint` package namespace, `viewlint` extension and task).
- **Ten Adaptive UI Static-Analysis Rules**: Full support for rules FF001–FF010 auditing Android Manifests, XML Layout resources, and Jetpack Compose `@Composable` functions.
- **Gradle Lifecycle Integration**: Automatic wiring into `./gradlew check` standard verification lifecycle with configurable quality gates (`NEVER`, `ERROR`, `WARNING`, `NEW`).
- **Multi-Format Reporting**: Export diagnostic reports in Console, JSON (`viewlint-report.json`), HTML (`viewlint-report.html`), and SARIF 2.1.0 (`viewlint-report.sarif`).
- **Baselines & Quality Control**: Baseline signature generation and suppression (`viewlint-baseline.txt`) for managing legacy technical debt.

### Engineering Improvements
- **FF002 Precedence & Target SDK Resolution**: Implemented accurate `resizeableActivity` evaluation order (activity attribute > application attribute > target SDK platform default).
- **Target-SDK-Dependent Defaults**: Correctly defaults `resizeableActivity` to `true` for `targetSdkVersion` >= 24 and `false` for `targetSdkVersion` < 24.
- **Conservative Fallback**: Uses a conservative fallback defaulting `resizeableActivity` to `false` when `targetSdkVersion` is missing or unparseable.
- **Android 16 Documentation**: Documented platform orientation and aspect-ratio restriction override behavior on Android 16 (API 36) for displays with smallest width >= 600dp.
- **Shaded Plugin Artifact**: Fully shaded self-contained Gradle plugin distribution bundling `viewlint-core` and `viewlint-analyzer` to prevent downstream dependency leaks.
- **Test Suite Coverage**: 36 comprehensive tests covering rule execution, report generation, configuration-cache compatibility, and target SDK precedence.

### Installation

```kotlin
plugins {
    id("io.github.dev-vikas-soni.viewlint") version "0.7.1"
}
```

### Known Limitations
- Viewlint performs static code analysis and does not guarantee runtime rendering compatibility or replace physical device testing.
- Target SDK resolution relies on `AndroidManifest.xml`; when omitted, FF002 defaults conservatively to `false`.
- Compose rules (FF007–FF010) use structural pattern heuristics and may require baseline suppression for complex custom layout implementations.

---

## [0.7.0] — Gradle Verification Lifecycle Integration
- Added automatic integration with `./gradlew check` verification lifecycle.
- Extended `ViewlintExtension` with `enabled` and `runOnCheck` configuration options.
- Refactored `ViewlintTask` to use modern Gradle lazy property APIs (`ListProperty`, `DirectoryProperty`, `RegularFileProperty`), achieving full configuration-cache compatibility.

## [0.6.0] — GitHub Actions & SARIF Integration
- Enhanced `SarifReportWriter` with repository-relative path normalization and `partialFingerprints` (`primaryLocationLineHash`) for stable finding identity across CI runs.
- Created official GitHub Actions workflow template (`.github/workflows/viewlint.yml`) for Code Scanning upload using `github/codeql-action/upload-sarif@v4`.

## [0.5.0] — Diagnostic Export & CI Reporting
- Introduced reporting package with multi-format exporters: Console, JSON, HTML, and SARIF 2.1.0.
- Implemented baseline creation, loading, and signature filtering (`ViewlintBaseline`).
- Implemented configurable quality gates (`NEVER`, `ERROR`, `WARNING`, `NEW`).

## [0.4.0] — Compose UI & Modifier Static Analysis
- Added Kotlin source discovery and Kotlin source parsing for `@Composable` functions.
- Implemented rules FF007 (Hardcoded dimensions), FF008 (Restrictive required size), FF009 (Missing adaptive handling), and FF010 (Restrictive modifier chains).

## [0.3.0] — Android Resource & Layout XML Static Analysis
- Implemented layout resource discovery across `res/layout*` directories.
- Created secure DOM layout parser (`LayoutParser`).
- Implemented rules FF003 (Fixed root width), FF004 (Fixed inner dimensions), FF005 (Missing adaptive variants), and FF006 (Absolute positioning).

## [0.2.0] — Manifest Analysis & Engine Stabilization
- Strengthened FF001 (Orientation restrictions) and implemented FF002 (Aspect ratio restrictions).
- Improved XML parsing safety and error handling.
- Added Gradle TestKit functional plugin tests.

## [0.1.0] — Core Foundation
- Initial core architecture (`ViewlintFinding`, `Severity`, `Confidence`, `ViewlintRule`, `ViewlintContext`, `ViewlintEngine`, `ViewlintRuleRegistry`).
- Basic Gradle plugin and task structure.
