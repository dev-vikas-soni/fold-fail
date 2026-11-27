# FoldFail — Android Adaptive Quality Auditor

[![FoldFail CI & Code Scanning](https://github.com/dev-vikas-soni/fold-fail/actions/workflows/foldfail.yml/badge.svg)](https://github.com/dev-vikas-soni/fold-fail/actions/workflows/foldfail.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

**FoldFail** is an open-source static analysis tool for Android designed to identify potential application compatibility and adaptive UI risks across phones, foldables, tablets, split-screen, resizable windows, and desktop windowing environments.

*FoldFail principle: Build for the window, not just the device.*

---

## Problem Statement

Modern Android applications run across a vast array of form factors—from compact smartphones and dual-screen foldables to large tablets, desktop windowing environments, and automotive displays. Hardcoded layout assumptions, explicit orientation locks, rigid aspect-ratio constraints, and fixed Compose modifier dimensions frequently result in letterboxing, pillarboxing, clipped UI, or poor multi-window experiences.

FoldFail bridges the gap between development and multi-window runtime testing by detecting these adaptive risks during static analysis before runtime deployment.

---

## What FoldFail Does & Does NOT Do

### What FoldFail Does
- Audits `AndroidManifest.xml` files for explicit activity orientation restrictions (FF001) and aspect-ratio constraints (FF002).
- Audits Android XML layout resources (`res/layout*`) for fixed root widths (FF003), restrictive large fixed inner dimensions (FF004), complex layouts lacking adaptive variants (FF005), and fragile absolute positioning (FF006).
- Audits Jetpack Compose source files (`.kt`) for hardcoded dimension modifiers (FF007), restrictive required sizes (FF008), complex composables lacking adaptive layout handling (FF009), and restrictive modifier chains (FF010).
- Generates multi-format diagnostic reports (`CONSOLE`, `JSON`, `HTML`, `SARIF 2.1.0`).
- Supports baseline comparisons (`foldfail-baseline.txt`) and configurable quality gates (`NEVER`, `ERROR`, `WARNING`, `NEW`).
- Integrates natively into the Gradle verification lifecycle (`./gradlew check`) and GitHub Actions Code Scanning.

### What FoldFail Does NOT Do
- FoldFail is **not** a Google certification tool and does not prove device compatibility.
- FoldFail does **not** perform runtime emulator measurements, ADB device inspection, or automated UI fixing.
- FoldFail does **not** replace Android Lint, Layout Inspector, or end-to-end device testing.

---

## Supported Analysis Areas & Rule Catalog

| Rule ID | Rule Name | Category | Description |
| :--- | :--- | :--- | :--- |
| **FF001** | Activity orientation is restricted | Manifest | Detects explicit activity `screenOrientation` restrictions. |
| **FF002** | Activity aspect ratio is restricted | Manifest | Detects `maxAspectRatio` or `minAspectRatio` when effective resizability is false. |
| **FF003** | Root layout has fixed width | XML Layout | Detects restrictive fixed root widths (`layout_width`) lacking adaptive variants. |
| **FF004** | Fixed large inner dimensions | XML Layout | Detects large fixed dimensions on non-scroll container children. |
| **FF005** | Complex layout lacks adaptive variants | XML Layout | Heuristic identifying complex layouts (>15 nodes) without qualified variants (`sw600dp`, etc.). |
| **FF006** | AbsoluteLayout or absolute coordinates | XML Layout | Detects deprecated `AbsoluteLayout` or `layout_x`/`layout_y` positioning. |
| **FF007** | Composable uses hardcoded dimensions | Compose | Detects fixed `width`, `height`, or `size` dp modifiers without adaptive cues. |
| **FF008** | Composable uses restrictive required size | Compose | Detects `requiredWidth`, `requiredHeight`, or `requiredSize` overriding constraints. |
| **FF009** | Missing adaptive layout handling | Compose | Heuristic identifying complex screen-level composables lacking adaptive branching. |
| **FF010** | Restrictive modifier chain combination | Compose | Detects combinations of fixed width and height or size modifiers. |

---

## Gradle Plugin Installation & Configuration

Add the plugin to your root or module `build.gradle.kts`:

```kotlin
plugins {
    id("io.github.dev-vikas-soni.foldfail") version "0.7.0"
}

foldFail {
    enabled = true
    runOnCheck = true
    reportFormats = listOf("console", "json", "html", "sarif")
    outputDir = layout.buildDirectory.dir("reports/foldfail").get().asFile
    failOn = "WARNING"
    baselineFile = file("foldfail-baseline.txt")
}
```

### Standalone Task Execution
```bash
./gradlew foldFail
```

### Gradle Verification Lifecycle Integration
When `runOnCheck = true` (default), running `./gradlew check` automatically executes FoldFail audits.

---

## GitHub Actions Integration

FoldFail outputs standard **SARIF 2.1.0** reports, which integrate natively with **GitHub Code Scanning**.

Create `.github/workflows/foldfail.yml`:

```yaml
name: FoldFail Adaptive Quality Audit

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
  foldfail-audit:
    name: Run FoldFail Audit & Upload SARIF
    runs-on: ubuntu-latest

    steps:
      - name: Checkout Repository
        uses: actions/checkout@v4

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: '21'
          cache: 'gradle'

      - name: Grant execute permission for gradlew
        run: chmod +x gradlew

      - name: Run FoldFail Audit
        run: ./gradlew foldFail

      - name: Upload SARIF to GitHub Code Scanning
        uses: github/codeql-action/upload-sarif@v3
        if: always()
        with:
          sarif_file: build/reports/foldfail/foldfail-report.sarif
          category: foldfail-audit
```

---

## Documentation & Roadmap

- [Overview](docs/overview.md)
- [Architecture](docs/architecture.md)
- [Rules Catalog](docs/rules-catalog.md)
- [Configuration](docs/configuration.md)
- [Reports & CI](docs/reports-and-ci.md)
- [Testing & Validation](docs/testing-and-validation.md)
- [Limitations](docs/limitations.md)
- [Roadmap](docs/roadmap.md)
- [Contributing](CONTRIBUTING.md)
- [Changelog](CHANGELOG.md)

---

## License

Distributed under the MIT License. See [LICENSE](LICENSE) for more information.
