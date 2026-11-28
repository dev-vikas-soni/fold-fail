# ViewLint — Evidence-Backed Adaptive UI Auditor for Android

[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/io.github.dev-vikas-soni.viewlint)](https://plugins.gradle.org/plugin/io.github.dev-vikas-soni.viewlint)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)
[![Build Status](https://img.shields.io/badge/build-passing-brightgreen.svg)]()

> **Evidence-backed adaptive UI diagnostics for Android XML and Jetpack Compose.**

ViewLint is a static analysis engine and Gradle plugin for Android that detects layout, dimension, orientation, and responsiveness risks across phones, foldables, tablets, split-screen windows, and desktop environments.

---

## 📱 The Android Adaptive UI Challenge

Android devices range from compact outer screens on foldables ($300\text{dp}$) to expanded inner displays ($600-840\text{dp}$), tablets, Chromebooks, and freeform resizable multi-window environments. 

Traditional layout code often contains subtle rigid assumptions:
- Hardcoded dp dimensions locking component bounds on wide displays.
- Activity orientation locks causing letterboxing on foldables.
- Fixed aspect-ratio constraints triggering pillarboxing.
- Required size modifiers overriding parent layout constraints.

ViewLint audits your project manifests, XML layouts, and Jetpack Compose codebases automatically during builds to catch these issues before they reach production.

---

## 🔍 What ViewLint Detects

ViewLint features **10 rules** (FF001 – FF010) covering Manifest, XML Layout, and Jetpack Compose analysis:

| Rule ID | Name | Target | Default Severity | Confidence | Description |
| :--- | :--- | :--- | :--- | :--- | :--- |
| [**FF001**](docs/rules/FF001.md) | Activity orientation is restricted | Manifest | `WARNING` | `MEDIUM` | Detects `android:screenOrientation` locks on activities. |
| [**FF002**](docs/rules/FF002.md) | Activity aspect ratio is restricted | Manifest | `WARNING` | `MEDIUM` | Detects `maxAspectRatio` or `minAspectRatio` bounds. |
| [**FF003**](docs/rules/FF003.md) | Root layout has fixed width | XML | `WARNING` | `MEDIUM` | Root view specifies hardcoded dp width without adaptive variants. |
| [**FF004**](docs/rules/FF004.md) | Fixed large inner dimensions | XML | `INFO` | `LOW` | Non-scroll inner view specifies fixed width & height $\ge 300\text{dp}$. |
| [**FF005**](docs/rules/FF005.md) | Complex layout lacks adaptive variants | XML | `INFO` | `LOW` | Complex XML layout (>15 nodes) lacking `-w600dp` or `-sw600dp` variants. |
| [**FF006**](docs/rules/FF006.md) | AbsoluteLayout or absolute coordinates | XML | `WARNING` | `HIGH` / `MEDIUM` | Deprecated `<AbsoluteLayout>` or `layout_x`/`layout_y` attributes. |
| [**FF007**](docs/rules/FF007.md) | Composable uses hardcoded dimensions | Compose | `WARNING` | `HIGH` / `MEDIUM` | Fixed `.width()`, `.height()`, or `.size()` dp modifiers on composables. |
| [**FF008**](docs/rules/FF008.md) | Composable uses restrictive required size | Compose | `WARNING` / `INFO` | `HIGH` / `LOW` | `.requiredWidth()`, `.requiredHeight()`, or `.requiredSize()` calls. |
| [**FF009**](docs/rules/FF009.md) | Missing adaptive layout handling | Compose | `INFO` | `LOW` | Screen composable without `WindowSizeClass` or `BoxWithConstraints`. |
| [**FF010**](docs/rules/FF010.md) | Restrictive modifier chain combination | Compose | `WARNING` | `HIGH` / `MEDIUM` | Modifier chain locking both width and height simultaneously. |

---

## ⚡ Key Capabilities

- **Jetpack Compose & XML Support**: Analyzes both modern Compose source code (`.kt`) and legacy XML layouts (`res/layout*`) plus `AndroidManifest.xml`.
- **Semantic Heuristic Engine**: Differentiates content containers, input surfaces (OTP cells/fields), and leaf graphic elements.
- **Evidence-Driven Findings**: Every finding specifies exact file paths, line numbers, evidence snippets, confidence levels, and remediation advice.
- **4 Output Formats**: Console output, interactive light/dark HTML report, structured JSON, and OASIS SARIF 2.1.0 for GitHub Code Scanning.
- **Baseline Support**: Record existing project findings into a baseline file to prevent build breaks while fixing technical debt.
- **Quality Gates**: Enforce build failure thresholds (`NEVER`, `ERROR`, `WARNING`, `NEW`) in CI pipelines.
- **Configuration Cache Compliant**: Fully compatible with Gradle configuration cache and multi-module builds.

---

## 🛠️ Installation & Setup

Apply the plugin in your root or module `build.gradle.kts`:

```kotlin
plugins {
    id("io.github.dev-vikas-soni.viewlint") version "1.0.0"
}
```

### Minimal Configuration Example

```kotlin
viewlint {
    // Fail build if WARNING or ERROR severity findings exist (Options: NEVER, ERROR, WARNING, NEW)
    failOn = "WARNING"

    // Enable reports (Options: console, json, html, sarif)
    reportFormats = listOf("console", "json", "html", "sarif")

    // Optional baseline file
    baselineFile = file("viewlint-baseline.txt")
}
```

Run the audit task:

```bash
./gradlew viewlint
```

---

## 📊 Sample Finding & Report Output

### Terminal Console Output

```
Viewlint — Android Adaptive Quality Auditor
============================================

[WARNING] FF007: Composable uses hardcoded dimensions
File: app/src/main/java/com/example/HomeScreen.kt
Evidence: width(340.dp)
Message: Composable 'MainCard' uses a fixed 340dp width ('width(340.dp)') on a layout region without detectable adaptive handling.
Confidence: HIGH
Remediation: Review whether 'MainCard' needs to expand across foldables, tablets, or split-screen windows. Consider using flexible modifiers (fillMaxWidth, weight) or adapting to WindowSizeClass.

--------------------------------------------
Analysis complete. Findings: 1
--------------------------------------------
```

### Generated Reports
Reports are saved in `build/reports/viewlint/`:
- `viewlint-report.html` — Interactive offline visual report with search, filtering, and theme toggle.
- `viewlint-report.json` — Machine-readable summary and findings array.
- `viewlint-report.sarif` — Native SARIF format for GitHub Security Code Scanning upload.

---

## 🛡️ Baseline & Quality Gate Usage

To adopt ViewLint incrementally on an existing codebase without failing CI:

```kotlin
viewlint {
    failOn = "NEW"
    baselineFile = file("config/viewlint-baseline.txt")
}
```

1. On the first run, ViewLint creates `config/viewlint-baseline.txt` and records all existing findings.
2. Subsequent runs filter out baselined findings and only fail if **new adaptive risks** are introduced.

---

## 📋 Compatibility Matrix

| Component | Minimum Version | Tested Version |
| :--- | :--- | :--- |
| **Gradle** | 8.0+ | 8.5, 9.5.0 |
| **Java / JDK** | JDK 17+ | OpenJDK 21 |
| **Kotlin** | 1.9.0+ | 2.0.21, 2.3.20 |
| **Android AGP** | 8.0+ | 8.2 – 9.0 |

---

## ⚠️ Limitations & False-Positive Scenarios

- **Calculated Modifiers**: Sizing calculations performed via custom helper methods outside standard `Modifier.width(x.dp)` syntax may not be statically extracted.
- **External Adaptive Logic**: If adaptive layout switching occurs in a parent composable in another file, ViewLint evaluates the child composable with `Confidence.LOW` / `Confidence.MEDIUM`.
- **Specialized UI**: Fixed-size canvas elements, map view containers, and custom OTP input cells are exempted where recognized, but unusual custom widgets may require baseline suppression.

---

## 🧪 Testing & Contributions

Run all project tests and benchmarks:

```bash
# Run unit tests and plugin tests
./gradlew test

# Run the 45-fixture evidence accuracy benchmark suite
./gradlew :viewlint-analyzer:test --tests com.viewlint.analyzer.benchmark.AccuracyBenchmarkTest

# Run performance benchmark suite
./benchmarks/run_performance_benchmark.sh
```

Contributions are welcome! Please review [CONTRIBUTING.md](CONTRIBUTING.md) and [docs/contributing/contribution-guidelines.md](docs/contributing/contribution-guidelines.md).

---

## 📚 Documentation Links

- [Getting Started Guide](docs/getting-started/quickstart.md)
- [Rule Catalog & Details](docs/rules/README.md)
- [Configuration Reference](docs/getting-started/configuration.md)
- [Baseline Guide](docs/guides/baseline.md)
- [CI/CD Integration](docs/guides/ci-integration.md)
- [Architecture & Engine Design](docs/engineering/architecture.md)
- [Changelog](CHANGELOG.md)
- [Issue Tracker](https://github.com/dev-vikas-soni/viewlint/issues)

---

## 📄 License

ViewLint is released under the [Apache License 2.0](LICENSE).
