# FoldFail Changelog

All notable changes to this project will be documented in this file.

## [0.7.0] — Gradle Verification Lifecycle Integration
- Added automatic integration with `./gradlew check` verification lifecycle.
- Extended `FoldFailExtension` with `enabled` and `runOnCheck` configuration options.
- Refactored `FoldFailTask` to use modern Gradle lazy property APIs (`ListProperty`, `DirectoryProperty`, `RegularFileProperty`), achieving full configuration-cache compatibility.

## [0.6.0] — GitHub Actions & SARIF Integration
- Enhanced `SarifReportWriter` with repository-relative path normalization and `partialFingerprints` (`primaryLocationLineHash`) for stable finding identity across CI runs.
- Created official GitHub Actions workflow template (`.github/workflows/foldfail.yml`) for Code Scanning upload using `github/codeql-action/upload-sarif@v3`.

## [0.5.0] — Diagnostic Export & CI Reporting
- Introduced reporting package with multi-format exporters: Console, JSON, HTML, and SARIF 2.1.0.
- Implemented baseline creation, loading, and signature filtering (`FoldFailBaseline`).
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
- Initial core architecture (`FoldFailFinding`, `Severity`, `Confidence`, `FoldFailRule`, `FoldFailContext`, `FoldFailEngine`, `FoldFailRuleRegistry`).
- Basic Gradle plugin and task structure.
