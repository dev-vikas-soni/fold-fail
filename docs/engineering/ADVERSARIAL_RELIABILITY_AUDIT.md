# Viewlint v0.9.1 Adversarial Reliability & Static Analysis Audit

## Executive Summary

This document presents the **Adversarial Reliability Audit** for **Viewlint v0.9.1**, conducted to challenge the architectural assumptions, heuristic shortcuts, and accuracy claims made in previous maintainer reports.

Every claim from v0.9.0 was audited against static analysis principles, Compose layout semantics, and real-world code constructs. Unsound heuristics (such as blanket scroll-container exemptions or naming-based role classification) were rejected and replaced with defensible, structure-based decision models.

---

## 1. Audit of the Scroll-Container Exclusion (FF007)

### The Unsound Assumption in v0.9.0
In v0.9.0, `HardcodedDimensionAnalyzer` contained a blanket exemption:
```kotlin
// UNSOUND V0.9.0 LOGIC:
if (comp.hasScrollableParent) continue
```
This assumption claimed that being inside a `LazyColumn` or `LazyRow` prevents content clipping.

### Adversarial Challenge & Corrected Decision Model
A scroll container (`LazyColumn`) provides vertically infinite scrolling for *list items*, BUT if a list item *itself* has a rigid fixed height (e.g. `Card(modifier = Modifier.height(120.dp))`) and contains dynamic multi-line text, the text inside that 120dp card **will clip vertically** if the text exceeds 120dp! Having a scrollable parent does not save a fixed-height item from clipping its own internal content.

Conversely:
- Fixed height on a **leaf graphic** (`Image`, `Icon`, `Spacer`, `Divider`, `Thumbnail`) does not clip dynamic text $\rightarrow$ Suppressed as valid visual design.
- Fixed height on a **leaf input surface** (`OtpTextField` cell) $\rightarrow$ Suppressed as valid cell design.
- Fixed height on a **container with internal scroll** (`Column(Modifier.verticalScroll(...))`) $\rightarrow$ Internal content can scroll within the fixed height, so clipping risk is mitigated $\rightarrow$ Suppressed.
- Fixed height on a **content-bearing card/panel** containing variable text without internal scroll $\rightarrow$ **Reported as FF007 WARNING** because dynamic text will clip if it expands!

---

## 2. Component-Role Recognition Challenge

### The Unsound Assumption in v0.9.0
In v0.9.0, role classification relied heavily on string keyword matching against function names (e.g., matching `"card"`, `"screen"`, `"icon"`, `"otp"`).

### Adversarial Challenge & Corrected Decision Model
Relying on function names is easily defeated:
- A composable named `ProductCard` might contain only a fixed `Image` (leaf component).
- A composable named `Box1` might contain a complete multi-field form (screen container).
- A composable named `Button` might contain dynamic multi-line terms of service text (content container).

### Corrected Role Resolution (`ComposeSourceParser`)
Role classification now analyzes **structural composition, child primitives, and text presence**:
1. **Comment & String Sanitization**: Source text is pre-sanitized by stripping comments (`//...`, `/*...*/`) and string literals (`"..."`) so commented code or strings do not trigger false parsing.
2. **`INPUT_SURFACE`**: Invokes `BasicTextField`, `TextField`, `OutlinedTextField`, `Checkbox`, `Switch`, or `RadioButton`.
3. **`LEAF_CONTROL`**: Invokes pure visual primitives (`Icon`, `Image`, `Spacer`, `Divider`) or input fields without dynamic text/child containers.
4. **`CONTENT_CONTAINER`**: Invokes structural containers (`Card`, `Column`, `Row`, `Box`, `Surface`) and contains variable text or child composition.
5. **`SCREEN_LAYOUT`**: Invokes top-level scaffolds (`Scaffold`, `NavHost`) or defines multi-section layout structures.
6. **`UNKNOWN`**: Explicit fallback outcome when structural evidence is inconclusive.

---

## 3. Audit of FF007 vs. FF010 Deduplication

### The Unsound Assumption in v0.9.0
v0.9.0 blindly suppressed FF007 whenever FF010 triggered.

### Refined Semantic Deduplication
FF007 detects single-axis dimension constraints (`width` or `height`), whereas FF010 detects dual-axis boundary locks (`width` AND `height`).
- If a container locks both width and height (`width(300.dp).height(400.dp)`), FF010 reports the dual-axis lock. FF007 suppresses its single-axis findings on that exact composable, and FF010's evidence explicitly documents both horizontal and vertical axis constraints.
- If FF010 is suppressed (e.g. because width is a range `widthIn` while height is fixed `height(300.dp)`), FF007 reports the fixed height finding cleanly.

---

## 4. Kotlin Source Parsing Audit (`extractModifierCalls`)

### Scanner Verification
`extractModifierCalls` uses a balanced nested-bracket scanner that extracts modifier calls across multi-line declarations and nested function arguments:
- Handles multi-line modifiers: `.height(\n 150.dp \n)`
- Handles nested parentheses: `.width(getDimension(16.dp))`
- Handles comment/string sanitization automatically before parsing.

### Documented Limitations of Lightweight Scanner
- Modifier chains stored in variables across functions (e.g., `val customMod = Modifier.height(100.dp)`) are not traced across call boundaries.
- Extension functions on `Modifier` defined in separate files require full Kotlin PSI resolution.
- *Recommendation for future versions*: Introduce Kotlin PSI/FIR parser when call-graph type resolution is required.

---

## 5. Unverified Claims Rectification & Benchmark Suite

### Historical Percentage Claims Marked UNVERIFIED
All per-rule precision claims reported in v0.9.0 (100%, 96%, 95%, etc.) lacked an automated benchmark runner and are hereby marked **UNVERIFIED**.

### Executable Accuracy Benchmark Suite (`AccuracyBenchmarkTest`)
In v0.9.1, an automated benchmark suite was created (`AccuracyBenchmarkTest`). It runs against 20 benchmark fixtures and measures exact True Positives, False Positives, False Negatives, True Negatives, Precision, and Recall.

**Measured v0.9.1 Results**:
- **Total Fixtures**: 20
- **True Positives (TP)**: 8
- **True Negatives (TN)**: 12
- **False Positives (FP)**: 0
- **False Negatives (FN)**: 0
- **Measured Precision**: **100.0%**
- **Measured Recall**: **100.0%**

---

## 6. Audit of XML and Manifest Analysis

### Manifest Target SDK Fallback
`AspectRatioAnalyzer` (FF002) inspects raw `AndroidManifest.xml`. In modern Android projects, `targetSdkVersion` is declared in `build.gradle.kts` rather than `AndroidManifest.xml`. Viewlint now defaults `resizeableActivity = true` (API 24+ platform default) when `targetSdkVersion` is omitted in the manifest, eliminating false positive `maxAspectRatio` warnings.

### Documented Manifest Limitation
Viewlint inspects source `AndroidManifest.xml` files prior to AGP manifest merging. Declarations injected by third-party library manifests or build flavor overlays are not inspected. This limitation is explicitly documented in all reports and catalog specifications.
