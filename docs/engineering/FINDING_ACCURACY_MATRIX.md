# Viewlint Finding Accuracy Matrix & Ground-Truth Test Corpus

## Executive Summary

This document establishes the ground-truth test corpus and evaluation matrix for Viewlint static analysis rules. The corpus consists of 25 benchmark test cases covering **Positive Fixtures** (valid code patterns that must not be reported as defects), **Negative Fixtures** (adaptive UI defects that must be reported with precise severity/confidence), and **Ambiguous Fixtures** (cases requiring honest uncertainty handling and reduced confidence).

---

## 1. Ground-Truth Test Matrix

| ID | Fixture Category | Rule Target | Code Construct / Scenario | Expected Outcome | Severity | Confidence | Rationale |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **FX-01** | Positive | FF010 | `OtpTextField` box: `width(16.dp).height(52.dp)` | **NO FINDING** | N/A | N/A | Leaf input cell with fixed character bounds. |
| **FX-02** | Positive | FF007 | `UserAvatar`: `size(40.dp)` | **NO FINDING** | N/A | N/A | Standard touch target / avatar icon ($\le 48\text{dp}$). |
| **FX-03** | Positive | FF007 | `ActionIcon`: `size(24.dp)` | **NO FINDING** | N/A | N/A | Standard vector icon sizing ($\le 48\text{dp}$). |
| **FX-04** | Positive | FF007 | `ProductThumbnail`: `size(60.dp)` on Image | **NO FINDING** | N/A | N/A | Standard thumbnail graphic on leaf component ($\le 64\text{dp}$). |
| **FX-05** | Positive | FF007 | `BannerAdminCard`: `height(150.dp)` in `LazyColumn` | **NO FINDING** | N/A | N/A | Card inside vertical scroll container has overflow path. |
| **FX-06** | Positive | FF007 | `FlexibleCard`: `widthIn(max = 400.dp)` | **NO FINDING** | N/A | N/A | Range constraint (`widthIn`) allows responsive shrinking. |
| **FX-07** | Positive | FF007 | `FlexiblePanel`: `heightIn(max = 300.dp)` | **NO FINDING** | N/A | N/A | Range constraint (`heightIn`) allows responsive sizing. |
| **FX-08** | Positive | FF007 | `SizeInBox`: `sizeIn(min = 100.dp, max = 200.dp)` | **NO FINDING** | N/A | N/A | `sizeIn` range constraint is responsive. |
| **FX-09** | Positive | FF009 | `ProductGrid`: `LazyVerticalGrid(GridCells.Adaptive(160.dp))` | **NO FINDING** | N/A | N/A | `GridCells.Adaptive` is explicit adaptive layout handling. |
| **FX-10** | Positive | FF009 | `MainScreen`: accepts `isExpandedScreen: Boolean` | **NO FINDING** | N/A | N/A | Adaptive state passed via parameter. |
| **FX-11** | Positive | FF009 | `AppScaffold`: uses `NavigationSuiteScaffold` | **NO FINDING** | N/A | N/A | Official Compose M3 adaptive navigation component. |
| **FX-12** | Positive | FF009 | `DetailScreen`: uses `ListDetailPaneScaffold` | **NO FINDING** | N/A | N/A | Canonical Compose M3 two-pane adaptive layout. |
| **FX-13** | Positive | FF003 | XML `activity_main.xml` with `layout-sw600dp/` variant | **NO FINDING** | N/A | N/A | Qualified resource alternative exists for tablets. |
| **FX-14** | Positive | FF003 | XML `dialog_confirm.xml`: `layout_width="320dp"` | **NO FINDING** | N/A | N/A | Intentionally constrained modal dialog root. |
| **FX-15** | Negative | FF007 | `AdminCard`: `height(150.dp)` on non-scroll Column | **REPORTED** | `WARNING` | `HIGH` | Rigid fixed height on unadapted content panel containing text. |
| **FX-16** | Negative | FF007 | `ProductEditSheet`: `height(200.dp)` without scroll | **REPORTED** | `WARNING` | `HIGH` | Fixed height on sheet container risks content truncation. |
| **FX-17** | Negative | FF008 | `Dashboard`: `requiredWidth(400.dp).requiredHeight(600.dp)` | **REPORTED** | `WARNING` | `HIGH` | `requiredSize` overrides parent incoming constraints on container. |
| **FX-18** | Negative | FF009 | `AdminNotificationsScreen`: 500 chars text, no adaptive cue | **REPORTED** | `INFO` | `LOW` | Screen container lacks responsive branching or adaptive grid. |
| **FX-19** | Negative | FF010 | `LockedCard`: `width(300.dp).height(400.dp)` non-leaf | **REPORTED** | `WARNING` | `HIGH` | Combines fixed width and height on non-leaf content container. |
| **FX-20** | Negative | FF001 | Manifest `<activity android:screenOrientation="portrait">` | **REPORTED** | `WARNING` | `MEDIUM` | Explicit orientation lock degrades foldable/multi-window UX. |
| **FX-21** | Negative | FF002 | Manifest `<activity android:maxAspectRatio="2.1">` | **REPORTED** | `WARNING` | `MEDIUM` | Aspect ratio lock letterboxes on wide tablet displays. |
| **FX-22** | Negative | FF003 | XML root `layout_width="400dp"` without qualifiers | **REPORTED** | `WARNING` | `MEDIUM` | Fixed root width prevents expansion on large screens. |
| **FX-23** | Negative | FF006 | XML layout with `<AbsoluteLayout>` | **REPORTED** | `WARNING` | `HIGH` | Deprecated absolute layout positioning. |
| **FX-24** | Ambiguous | FF007 | Custom modifier `Modifier.customCardSize()` | **REPORTED (LOW CONF)** | `WARNING` | `LOW` | Custom modifier implementation cannot be resolved statically. |
| **FX-25** | Ambiguous | FF009 | Screen composable delegating UI to helper function | **REPORTED (LOW CONF)** | `INFO` | `LOW` | Static analysis cannot verify helper function implementation. |

---

## 2. Accuracy Evaluation & Benchmark Protocol

To ensure data-driven release evaluation:
1. **Precision**: $\frac{\text{True Positives}}{\text{True Positives} + \text{False Positives}}$
2. **Recall**: $\frac{\text{True Positives}}{\text{True Positives} + \text{False Negatives}}$

### Target Quality Gates for Release v0.9.0
- **Precision target**: $\ge 95\%$ on the 25-fixture benchmark corpus.
- **False positive target**: $0$ false positives on `OtpTextField`, avatars, icons, and `GridCells.Adaptive`.
- **Regression suite**: Automated execution in `ComposeAnalyzersTest.kt`.
