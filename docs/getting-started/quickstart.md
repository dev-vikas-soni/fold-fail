# Quickstart Guide

Get up and running with ViewLint in 2 minutes.

## 1. Apply Plugin
Add ViewLint to your app's `build.gradle.kts`:

```kotlin
plugins {
    id("io.github.dev-vikas-soni.viewlint") version "1.0.0"
}
```

## 2. Run Audit
Execute the `viewlint` task from terminal:

```bash
./gradlew viewlint
```

## 3. Review Findings
By default, ViewLint prints findings to the console and generates reports in `build/reports/viewlint/`:
- `viewlint-report.html` (Interactive visual report with dark/light themes)
- `viewlint-report.json` (Machine-readable JSON)
- `viewlint-report.sarif` (OASIS SARIF 2.1.0 for GitHub Code Scanning)

## 4. Example Output

```
Viewlint — Android Adaptive Quality Auditor
============================================

[WARNING] FF007: Composable uses hardcoded dimensions
File: app/src/main/java/com/example/HomeScreen.kt
Evidence: width(320.dp)
Message: Composable 'HeaderCard' uses a fixed 320dp width ('width(320.dp)') on a layout region without detectable adaptive handling.
Confidence: HIGH
Remediation: Review whether 'HeaderCard' needs to expand across foldables, tablets, or split-screen windows.

--------------------------------------------
Analysis complete. Findings: 1
--------------------------------------------
```
