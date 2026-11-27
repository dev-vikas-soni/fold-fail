# Testing & Validation Guide

Viewlint is extensively tested using JUnit 4 unit tests and Gradle TestKit functional integration tests.

---

## Test Suites & Coverage

### 1. Analyzer Unit Tests
- **`OrientationRestrictionAnalyzerTest`**: Verifies detection of portrait restrictions, multiple activities, unspecified orientations, malformed XML, and missing manifests.
- **`AspectRatioAnalyzerTest`**: Verifies detection of `maxAspectRatio`/`minAspectRatio`, effective resizability overrides, malformed XML, and missing manifests.
- **`LayoutAnalyzersTest`**: Verifies fixed root width detection, adaptive variant suppression (FF003), large fixed inner dimensions (FF004), and absolute positioning (`AbsoluteLayout`, FF006).
- **`ComposeAnalyzersTest`**: Verifies hardcoded dp dimensions in Compose (FF007), required size modifiers (FF008), restrictive modifier chains (FF010), and adaptive cue suppression (`WindowSizeClass`).

### 2. Core Reporting Unit Tests
- **`ReportingTest` & `SarifReportTest`**: Verifies JSON serialization, HTML generation, SARIF 2.1.0 structure, path normalization, and partial fingerprint generation.

### 3. Gradle TestKit Functional Tests
- **`ViewlintPluginTest`**: Verifies Gradle plugin task execution, JSON/HTML/SARIF report generation, quality gate threshold enforcement, `./gradlew check` verification lifecycle integration, and configuration-cache reuse (`--configuration-cache`).

---

## Verified Build Commands

To clean, test, and build the entire project locally:
```bash
./gradlew clean test build
```
*Result*: Verified **BUILD SUCCESSFUL** across all modules and tasks.

To verify Configuration Cache compatibility:
```bash
./gradlew check --configuration-cache
```
*Result*: Verified configuration cache successfully stored and reused.
