<p align="center">
  <picture>
    <source media="(prefers-color-scheme: dark)" srcset="assets/branding/viewlint-logo-dark.svg">
    <source media="(prefers-color-scheme: light)" srcset="assets/branding/viewlint-logo-light.svg">
    <img alt="Viewlint — Android Adaptive Quality Auditor" src="assets/branding/viewlint-logo.svg" width="480">
  </picture>
</p>

<p align="center">
  <strong>Static analysis for Android adaptive UI risks across phones, tablets, foldables, split-screen, and resizable windows.</strong>
</p>

<p align="center">
  <a href="https://github.com/dev-vikas-soni/viewlint/actions/workflows/viewlint.yml"><img src="https://github.com/dev-vikas-soni/viewlint/actions/workflows/viewlint.yml/badge.svg" alt="Viewlint CI &amp; Code Scanning"></a>
  <a href="https://opensource.org/licenses/MIT"><img src="https://img.shields.io/badge/License-MIT-yellow.svg" alt="License: MIT"></a>
</p>

---

## What Viewlint Does

Modern Android applications run across a vast array of form factors—from compact smartphones and dual-screen foldables to large tablets, desktop windowing environments, and automotive displays. Hardcoded layout assumptions, explicit orientation locks, rigid aspect-ratio constraints, and fixed Compose modifier dimensions frequently result in letterboxing, pillarboxing, clipped UI, or poor multi-window experiences.

**Viewlint** provides static analysis for Android adaptive UI risks. It analyzes Android manifests, XML layout resources, and Jetpack Compose Kotlin source code to identify architectural assumptions that hinder responsive screen adaptation before runtime deployment.

---

## Installation

Add the Viewlint Gradle plugin from the Gradle Plugin Portal to your module or root `build.gradle.kts`:

```kotlin
plugins {
    id("io.github.dev-vikas-soni.viewlint") version "0.9.0"
}
```

---

## Quick Start

Run the standalone audit task:

```bash
./gradlew viewlint
```

---

## Gradle Lifecycle Integration

By default (`runOnCheck = true`), Viewlint hooks into Gradle's standard verification lifecycle:

```bash
./gradlew check
```

Executing `./gradlew check` automatically runs Viewlint audits alongside your unit tests and standard verification tasks.

---

## Rules

Viewlint registers ten static analysis rules across Android Manifest, XML Layout, and Jetpack Compose surfaces.

### Manifest & XML Layout Analysis (FF001–FF006)

| Rule ID | Rule Name | Severity | Description & Behavior |
| :--- | :--- | :--- | :--- |
| **FF001** | Activity orientation is restricted | `WARNING` | Detects explicit activity `screenOrientation` restrictions (`portrait`, `landscape`, `locked`, etc.) that prevent orientation changes and window resizing. *(Note: On Android 16 (API 36), orientation restrictions are ignored on form factors with smallest width >= 600dp).* |
| **FF002** | Activity aspect ratio is restricted | `WARNING` | Detects `maxAspectRatio` or `minAspectRatio` when effective resizability is false. Effective resizability considers explicit activity `resizeableActivity`, application `resizeableActivity`, and target SDK platform defaults (>= 24 defaults to `true`, < 24 or unresolved defaults conservatively to `false`). *(Note: On Android 16 (API 36), aspect-ratio restrictions are ignored on displays with smallest width >= 600dp unless opted out).* |
| **FF003** | Root layout has fixed width | `WARNING` | Detects root view elements in layout XML files specifying hardcoded `dp` widths lacking qualified adaptive layout variants (`-sw600dp`, `-w600dp`, `-land`, etc.). |
| **FF004** | Fixed large inner dimensions | `INFO` | Detects non-scroll container child view elements specifying both `layout_width` and `layout_height` as hardcoded values >= 300dp. |
| **FF005** | Complex layout lacks adaptive variants | `INFO` | Heuristic detecting complex layouts (>15 DOM nodes) that only have a single default layout variant without responsive alternatives. |
| **FF006** | AbsoluteLayout or absolute coordinates | `WARNING` | Detects usage of deprecated `<AbsoluteLayout>` or absolute coordinate attributes `layout_x`/`layout_y`. |

### Jetpack Compose Analysis (FF007–FF010)

| Rule ID | Rule Name | Severity | Description & Behavior |
| :--- | :--- | :--- | :--- |
| **FF007** | Composable uses hardcoded dimensions | `WARNING` | Detects `@Composable` functions using fixed `.width(...)`, `.height(...)`, or `.size(...)` `dp` modifiers without detectable adaptive cues (`WindowSizeClass`, `BoxWithConstraints`, or conditional sizing branches). |
| **FF008** | Composable uses restrictive required size | `WARNING` | Detects `@Composable` functions using `.requiredWidth(...)`, `.requiredHeight(...)`, or `.requiredSize(...)` modifiers that force dimensions regardless of incoming parent constraints. |
| **FF009** | Missing adaptive layout handling | `INFO` | Heuristic detecting complex screen-level `@Composable` functions that lack responsive layout branching or window size class awareness. |
| **FF010** | Restrictive modifier chain combination | `WARNING` | Detects combinations of fixed width and height modifiers or fixed `.size(...)` modifiers without adaptive cues that lock component boundaries. |

---

## Reports

Viewlint generates comprehensive diagnostic reports in standard formats.

Report files are output by default to:

```text
build/reports/viewlint/viewlint-report.json
build/reports/viewlint/viewlint-report.html
build/reports/viewlint/viewlint-report.sarif
```

Supported formats:
- **Console (`console`)**: Detailed terminal output showing finding details, evidence, and remediation advice.
- **JSON (`json`)**: Structured dataset containing metadata, summary statistics, and complete finding properties.
- **HTML (`html`)**: Interactive standalone summary dashboard with severity badges and searchable findings.
- **SARIF 2.1.0 (`sarif`)**: Industry-standard Static Analysis Results Interchange Format for GitHub Code Scanning.

---

## Configuration

Configure Viewlint using the `viewlint` extension block in your build script:

```kotlin
viewlint {
    // Enable or disable Viewlint static analysis (default: true)
    enabled = true

    // Wire viewlint task into standard ./gradlew check lifecycle (default: true)
    runOnCheck = true

    // Exported report formats: console, json, html, sarif
    reportFormats = listOf("console", "json", "html", "sarif")

    // Output directory for report files
    outputDir = layout.buildDirectory.dir("reports/viewlint").get().asFile

    // Quality gate failure threshold: NEVER, ERROR, WARNING, NEW
    failOn = "WARNING"

    // Optional baseline file for suppressing known findings
    baselineFile = file("viewlint-baseline.txt")
}
```

---

## CI Integration

Viewlint outputs SARIF 2.1.0, enabling direct integration with GitHub Code Scanning.

Here is a complete GitHub Actions workflow (`.github/workflows/viewlint.yml`):

```yaml
name: Viewlint Adaptive Quality Audit

on:
  push:
    branches: [ "main", "master" ]
  pull_request:
    branches: [ "main", "master" ]

permissions:
  contents: read
  security-events: write
  actions: read

jobs:
  viewlint-audit:
    name: Run Viewlint Audit & Upload SARIF
    runs-on: ubuntu-latest

    steps:
      - name: Checkout Repository
        uses: actions/checkout@v4

      - name: Set up JDK 21
        uses: actions/setup-java@v5
        with:
          distribution: 'temurin'
          java-version: '21'
          cache: 'gradle'

      - name: Grant execute permission for gradlew
        run: chmod +x gradlew

      - name: Run Viewlint Audit
        run: ./gradlew viewlint

      - name: Verify SARIF Report Generation
        run: test -f build/reports/viewlint/viewlint-report.sarif

      - name: Upload SARIF to GitHub Code Scanning
        uses: github/codeql-action/upload-sarif@v4
        if: always()
        with:
          sarif_file: build/reports/viewlint/viewlint-report.sarif
          category: viewlint-audit
```

---

## Limitations

- **Static Analysis**: Viewlint performs static code and manifest inspection. It cannot observe runtime state flows, dynamic window resizes, or custom view measurement logic.
- **Does Not Guarantee Adaptive Compatibility**: Passing Viewlint audits indicates absence of common static anti-patterns, but does not guarantee perfect adaptive UI rendering.
- **Does Not Replace Android Lint**: Viewlint focuses exclusively on adaptive UI risks and works alongside Android Lint rather than replacing it.
- **Does Not Replace Physical Testing**: Viewlint cannot replace testing on physical devices, emulators, or multi-window preview environments.
- **Heuristics**: Rules FF005 and FF009 rely on structural heuristics and pattern detection; valid design patterns may occasionally require baseline suppression.
- **Target SDK Fallback in FF002**: When `targetSdkVersion` cannot be resolved in `AndroidManifest.xml`, FF002 uses a conservative fallback defaulting `resizeableActivity` to `false` to ensure potential restrictions are audited.
- **Android 16 Platform Behavior**: On Android 16 (API 36), `screenOrientation` and aspect-ratio restrictions are ignored on displays with smallest width >= 600dp unless opted out, but restrictions remain active on compact screens and lower API levels.

---

## License

Distributed under the MIT License. See [LICENSE](LICENSE) for details.
