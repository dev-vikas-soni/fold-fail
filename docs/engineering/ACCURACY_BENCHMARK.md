# Viewlint Accuracy Benchmark Report & Methodology

## Executive Summary

To replace historical unverified accuracy figures with reproducible data, Viewlint v0.9.1 introduces an executable **Accuracy Benchmark Suite** (`AccuracyBenchmarkTest.kt`). 

The benchmark suite evaluates static analysis rules against a ground-truth corpus of 20 test fixtures covering Compose, XML layouts, and Android Manifests.

---

## 1. Ground-Truth Fixture Evaluation Matrix

| ID | Rule | Category | Code Scenario | Expected Result | Observed Result | Status |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **FX-01** | FF010 | Positive | `OtpTextField` cell: `width(16.dp).height(52.dp)` | No Finding | No Finding | **PASS (TN)** |
| **FX-02** | FF007 | Positive | `UserAvatar`: `size(40.dp)` | No Finding | No Finding | **PASS (TN)** |
| **FX-03** | FF007 | Positive | `ActionIcon`: `size(24.dp)` | No Finding | No Finding | **PASS (TN)** |
| **FX-04** | FF007 | Positive | `ProductThumbnail`: `size(60.dp)` Image | No Finding | No Finding | **PASS (TN)** |
| **FX-05** | FF007 | Positive | `FlexibleCard`: `widthIn(max = 400.dp)` | No Finding | No Finding | **PASS (TN)** |
| **FX-06** | FF007 | Positive | `FlexiblePanel`: `heightIn(max = 300.dp)` | No Finding | No Finding | **PASS (TN)** |
| **FX-07** | FF007 | Positive | `SizeInBox`: `sizeIn(min = 100.dp, max = 200.dp)` | No Finding | No Finding | **PASS (TN)** |
| **FX-08** | FF009 | Positive | `ProductGrid`: `LazyVerticalGrid(GridCells.Adaptive(160.dp))` | No Finding | No Finding | **PASS (TN)** |
| **FX-09** | FF009 | Positive | `MainScreen`: accepts `isExpandedScreen: Boolean` | No Finding | No Finding | **PASS (TN)** |
| **FX-10** | FF009 | Positive | `AppScaffold`: uses `NavigationSuiteScaffold` | No Finding | No Finding | **PASS (TN)** |
| **FX-11** | FF009 | Positive | `DetailScreen`: uses `ListDetailPaneScaffold` | No Finding | No Finding | **PASS (TN)** |
| **FX-12** | FF003 | Positive | XML layout with `layout-sw600dp/` variant | No Finding | No Finding | **PASS (TN)** |
| **FX-13** | FF007 | Negative | `AdminCard`: `height(150.dp)` on non-scroll Column with text | Finding Reported | Finding Reported | **PASS (TP)** |
| **FX-14** | FF007 | Negative | `ProductEditSheet`: `height(200.dp)` with text without scroll | Finding Reported | Finding Reported | **PASS (TP)** |
| **FX-15** | FF008 | Negative | `Dashboard`: `requiredWidth(400.dp).requiredHeight(600.dp)` | Finding Reported | Finding Reported | **PASS (TP)** |
| **FX-16** | FF009 | Negative | `AdminNotificationsScreen`: 500 chars text, no adaptive cue | Finding Reported | Finding Reported | **PASS (TP)** |
| **FX-17** | FF010 | Negative | `LockedCard`: `width(320.dp).height(480.dp)` non-leaf | Finding Reported | Finding Reported | **PASS (TP)** |
| **FX-18** | FF001 | Negative | Manifest `<activity android:screenOrientation="portrait">` | Finding Reported | Finding Reported | **PASS (TP)** |
| **FX-19** | FF002 | Negative | Manifest `<activity android:maxAspectRatio="2.1" resizeableActivity="false">` | Finding Reported | Finding Reported | **PASS (TP)** |
| **FX-20** | FF006 | Negative | XML layout with `<AbsoluteLayout>` | Finding Reported | Finding Reported | **PASS (TP)** |

---

## 2. Benchmark Metrics & Formulas

$$\text{Precision} = \frac{\text{True Positives}}{\text{True Positives} + \text{False Positives}} = \frac{8}{8 + 0} = 100.0\%$$

$$\text{Recall} = \frac{\text{True Positives}}{\text{True Positives} + \text{False Negatives}} = \frac{8}{8 + 0} = 100.0\%$$

$$\text{F1 Score} = 2 \cdot \frac{\text{Precision} \cdot \text{Recall}}{\text{Precision} + \text{Recall}} = 100.0\%$$

---

## 3. How to Run the Benchmark

The benchmark is integrated into the automated Gradle test task and can be executed via:

```bash
./gradlew :viewlint-analyzer:test --tests "com.viewlint.analyzer.benchmark.AccuracyBenchmarkTest"
```
