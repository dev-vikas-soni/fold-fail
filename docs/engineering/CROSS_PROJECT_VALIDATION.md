# Cross-Project Validation Report — ViewLint v0.9.2

## Executive Summary

To establish whether ViewLint's static analysis generalizes beyond synthetic unit fixtures, we executed ViewLint across representative real-world Android open-source repositories and multi-module sample applications.

This validation evaluates ViewLint against real-world Jetpack Compose, XML Layout, and Manifest configurations, documenting:
1. True positive defect detection in real codebases.
2. False positive friction points caused by static text parsing and keyword heuristics.
3. False negatives resulting from dynamic modifier delegation and extension functions.
4. Parsing limitations encountered during build execution.

---

## 1. Project Evaluation Scope & Target Matrix

| Project Name | Pinned Commit SHA | AGP / Gradle | Kotlin | Compose Version | Target SDK / Modules | Analyzed Scope |
|---|---|---|---|---|---|---|
| **DesiShuddh** (Production Sample) | `3a9f12b8` | AGP 8.2.2 / Gradle 8.5 | 1.9.22 | 1.6.0 | SDK 34 (2 modules) | Full Kotlin/XML source |
| **NowInAndroid** (Sample Target) | `7f4e912a` | AGP 8.4.0 / Gradle 8.6 | 1.9.23 | 1.6.4 | SDK 34 (32 modules) | Core UI & Feature modules |
| **ViewLint Orientation Sample** | `16ab9fb1` | AGP 8.3.0 / Gradle 8.5 | 2.3.20 | 1.6.0 | SDK 34 (1 module) | Sample Manifest & Layouts |

---

## 2. Empirical Audit Findings & Report Categorization

### Analysis Run Summary

Across the evaluated project modules, ViewLint generated **14 total raw findings**:

| Rule ID | Rule Name | Evaluated Codebase | Raw Findings | Verified TP | False Positives (FP) | False Negatives (FN) |
|---|---|---|---|---|---|---|
| `FF001` | Orientation Restriction | ViewLint Sample / DesiShuddh | 2 | 2 | 0 | 0 |
| `FF002` | Aspect Ratio Lock | DesiShuddh | 1 | 1 | 0 | 0 |
| `FF003` | Fixed Root Width | DesiShuddh Layouts | 2 | 1 | 1 | 0 |
| `FF004` | Fixed Inner Dimension | XML Layouts | 1 | 1 | 0 | 1 |
| `FF005` | Missing Adaptive Variants | XML Layouts | 2 | 1 | 1 | 1 |
| `FF006` | Fragile Layout Positioning | XML Layouts | 1 | 1 | 0 | 0 |
| `FF007` | Hardcoded Dimensions (Compose) | NowInAndroid / DesiShuddh | 3 | 2 | 1 | 2 |
| `FF008` | Required Size Overrides | Compose Features | 0 | 0 | 0 | 1 |
| `FF009` | Missing Adaptive Handling | Compose Screens | 1 | 1 | 0 | 1 |
| `FF010` | Restrictive Modifier Chains | Compose Components | 1 | 1 | 0 | 1 |
| **Total** | | | **14** | **11** | **3** | **7** |

---

## 3. In-Depth Analysis of Discrepancies & Limitations

### 1. Verified True Positives (TP)
- **`FF001` (Activity Orientation Lock)**: Correctly flagged `<activity android:screenOrientation="portrait">` in `samples/orientation-issue/src/main/AndroidManifest.xml` and production manifests. This represents a genuine adaptive risk on foldables and tablet multi-window setups.
- **`FF007` (Hardcoded Container Dimension)**: Correctly flagged fixed `height(150.dp)` on non-scrolling card containers containing variable description text.
- **`FF010` (Dual-Axis Modifier Lock)**: Correctly detected fixed `width(320.dp).height(480.dp)` locking both dimensions of an unscrollable form container.

### 2. Investigated False Positives (FP)
- **Keyword Over-Classification in `FF007` / `FF009`**:
  - *Symptom*: A custom component named `UserAvatarScreen()` containing a fixed `40.dp` avatar icon was flagged by `FF007` because its function name contained `"screen"`, causing `isScreenOrContainer` to evaluate to `true`.
  - *Root Cause*: `ComposeSourceParser` uses string keyword matching (`screenOrContainerKeywords`) on function names and body text.
- **Adaptive Variant Resolution in `FF003` / `FF005`**:
  - *Symptom*: Root layouts using multi-module resource overlays (`@dimen/root_width`) were flagged as fixed root width because `LayoutParser` evaluates raw XML string attributes rather than resolved resource values across merged Gradle modules.

### 3. Identified False Negatives (FN)
- **Modifier Variable Indirection**:
  ```kotlin
  val containerModifier = Modifier.width(350.dp).height(400.dp)
  Box(modifier = containerModifier) { Text("Dynamic Content") }
  ```
  - *Symptom*: ViewLint failed to detect the restrictive dimension in `Box` because `containerModifier` was defined as a local variable before being passed to `Box`.
  - *Root Cause*: `ComposeSourceParser` scans text per-composable function but does not construct a full PSI control flow graph or resolve variable references.
- **Custom Modifier Extension Functions**:
  ```kotlin
  fun Modifier.cardBounds() = this.width(320.dp).height(200.dp)
  ```
  - *Symptom*: ViewLint does not flag `.cardBounds()` at composable call sites because the hardcoded `.width(...)` call is hidden inside an extension function definition.

---

## 4. Architectural Limitations & Mitigations

> [!WARNING]
> ViewLint's current text-based parser (`ComposeSourceParser`) operates without Kotlin compiler symbol resolution (PSI / FIR). Consequently:
> 1. Variable modifier chains are un-tracked across function boundaries.
> 2. Function name heuristics can be fooled by misleading naming patterns.
> 3. Merged AndroidManifest files across library modules are not evaluated unless explicitly passed to `ViewlintContext`.

To mitigate these limitations in production pipelines:
- Use `viewlint.baselineFile` to suppress false positives on legacy codebases.
- Annotate or name reusable leaf components cleanly to avoid container keyword collisions.
- Refer to `PARSING_COMPATIBILITY_MATRIX.md` for exact parsing boundaries.
