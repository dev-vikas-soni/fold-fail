# Independent Audit of ViewLint v0.9.1 Benchmark & Expanded v0.9.2 Benchmark Corpus

## Executive Summary

This document presents an independent, critical audit of the **ViewLint v0.9.1 Accuracy Benchmark** and documents the design, ground truth, and execution results of the expanded **v0.9.2 Benchmark Corpus**.

The v0.9.1 release claimed 100% Precision, 100% Recall, and 100% F1-score based on a 20-fixture corpus (`FX-01` through `FX-20`). Our independent audit reveals that while the mathematical calculation on those 20 fixtures was correct, the claim of "100% rule accuracy" was **methodologically flawed** and **over-fit** due to severe coverage gaps:
1. **Zero Fixtures** were present for `FF004` (Fixed Inner Dimension) and `FF005` (Missing Adaptive Resource Variants).
2. **Single-Fixture Coverage** existed for `FF001`, `FF002`, `FF006`, `FF008`, and `FF010`.
3. **Synthetic Bias**: Fixtures were hand-authored to mirror internal heuristic strings rather than real-world Kotlin syntax variants.

To establish semantic soundness, ViewLint v0.9.2 expands the benchmark suite to **40 distinct, multi-paradigm fixtures** (`FX-01` through `FX-40`), testing all 10 rules across positive, negative, and edge cases.

---

## 1. Audit of the v0.9.1 20-Fixture Corpus

### Fixture-by-Fixture Mapping & Ground Truth Audit

| Fixture ID | Target Rule | Ground Truth | Expected Finding | Actual Finding | Expected Severity | Actual Severity | Confidence | Source | Independent Review |
|---|---|---|---|---|---|---|---|---|---|
| **FX-01** | `FF010` | Positive (Valid) | None | None | N/A | N/A | High | Synthetic | Audited (v0.9.2) |
| **FX-02** | `FF007` | Positive (Valid) | None | None | N/A | N/A | High | Synthetic | Audited (v0.9.2) |
| **FX-03** | `FF007` | Positive (Valid) | None | None | N/A | N/A | High | Synthetic | Audited (v0.9.2) |
| **FX-04** | `FF007` | Positive (Valid) | None | None | N/A | N/A | High | Synthetic | Audited (v0.9.2) |
| **FX-05** | `FF007` | Positive (Valid) | None | None | N/A | N/A | High | Synthetic | Audited (v0.9.2) |
| **FX-06** | `FF007` | Positive (Valid) | None | None | N/A | N/A | High | Synthetic | Audited (v0.9.2) |
| **FX-07** | `FF007` | Positive (Valid) | None | None | N/A | N/A | High | Synthetic | Audited (v0.9.2) |
| **FX-08** | `FF009` | Positive (Valid) | None | None | N/A | N/A | High | Synthetic | Audited (v0.9.2) |
| **FX-09** | `FF009` | Positive (Valid) | None | None | N/A | N/A | High | Synthetic | Audited (v0.9.2) |
| **FX-10** | `FF009` | Positive (Valid) | None | None | N/A | N/A | High | Synthetic | Audited (v0.9.2) |
| **FX-11** | `FF009` | Positive (Valid) | None | None | N/A | N/A | High | Synthetic | Audited (v0.9.2) |
| **FX-12** | `FF003` | Positive (Valid) | None | None | N/A | N/A | High | Synthetic | Audited (v0.9.2) |
| **FX-13** | `FF007` | Negative (Defect)| FF007 | FF007 | WARNING | WARNING | High | Synthetic | Audited (v0.9.2) |
| **FX-14** | `FF007` | Negative (Defect)| FF007 | FF007 | WARNING | WARNING | High | Synthetic | Audited (v0.9.2) |
| **FX-15** | `FF008` | Negative (Defect)| FF008 | FF008 | WARNING | WARNING | High | Synthetic | Audited (v0.9.2) |
| **FX-16** | `FF009` | Negative (Defect)| FF009 | FF009 | INFO | INFO | Low | Synthetic | Audited (v0.9.2) |
| **FX-17** | `FF010` | Negative (Defect)| FF010 | FF010 | WARNING | WARNING | Medium | Synthetic | Audited (v0.9.2) |
| **FX-18** | `FF001` | Negative (Defect)| FF001 | FF001 | WARNING | WARNING | Medium | Synthetic | Audited (v0.9.2) |
| **FX-19** | `FF002` | Negative (Defect)| FF002 | FF002 | WARNING | WARNING | Medium | Synthetic | Audited (v0.9.2) |
| **FX-20** | `FF006` | Negative (Defect)| FF006 | FF006 | WARNING | WARNING | High | Synthetic | Audited (v0.9.2) |

### Methodological Evaluation of v0.9.1 Claims

1. **Mathematical vs Methodological Accuracy**:
   - On the 20 fixtures tested, $TP = 8, TN = 12, FP = 0, FN = 0$.
   - $\text{Precision} = \frac{8}{8 + 0} = 100\%$, $\text{Recall} = \frac{8}{8 + 0} = 100\%$.
   - **Flaw**: Claiming $100\%$ accuracy for the entire ViewLint analyzer from this sample is statistically invalid because rules `FF004` and `FF005` had zero test instances, meaning their real-world recall was completely unmeasured.

2. **Absence of Negative and Positive Control Balance**:
   - `FF001` had 0 positive fixtures (only 1 negative).
   - `FF002` had 0 positive fixtures (only 1 negative).
   - `FF003` had 0 negative fixtures (only 1 positive).
   - `FF004` had 0 positive and 0 negative fixtures.
   - `FF005` had 0 positive and 0 negative fixtures.
   - `FF006` had 0 positive fixtures (only 1 negative).

---

## 2. Expanded v0.9.2 Benchmark Matrix (`FX-01` – `FX-40`)

To rectify these deficiencies, the benchmark suite in `AccuracyBenchmarkTest.kt` was expanded to **40 fixtures**, providing balanced positive and negative evaluation for all 10 rules:

| Rule ID | Rule Name | Positive Fixtures (Valid) | Negative Fixtures (Defects) | Total |
|---|---|---|---|---|
| **FF001** | Orientation Restriction | FX-21 | FX-18 | 2 |
| **FF002** | Aspect Ratio Lock | FX-22 | FX-19 | 2 |
| **FF003** | Fixed Root Width | FX-12, FX-23 | FX-24 | 3 |
| **FF004** | Fixed Inner Dimension | FX-25 | FX-26 | 2 |
| **FF005** | Missing Adaptive Variants | FX-27 | FX-28 | 2 |
| **FF006** | Fragile Layout Positioning | FX-29 | FX-20, FX-30 | 3 |
| **FF007** | Hardcoded Dimensions (Compose)| FX-02..07, FX-31 | FX-13, FX-14, FX-40 | 10 |
| **FF008** | Required Size Overrides | FX-32 | FX-15, FX-36, FX-38 | 4 |
| **FF009** | Missing Adaptive Handling | FX-08..11, FX-33, FX-35 | FX-16 | 7 |
| **FF010** | Restrictive Modifier Chains | FX-01, FX-34, FX-37 | FX-17, FX-39 | 5 |
| **Total** | **All 10 Rules** | **24** | **16** | **40** |

### Key New Edge Cases Introduced in v0.9.2:
- **`FX-24`**: Root `<LinearLayout>` with fixed `360dp` width lacking `layout-sw600dp` or `layout-w600dp` variants.
- **`FX-26`**: Inner layout container with `350dp` x `400dp` hardcoded dimensions on non-scroll layout.
- **`FX-28`**: Complex XML layout (>15 nodes) lacking alternative adaptive resources.
- **`FX-30`**: Absolute positioning via `android:layout_x` and `android:layout_y` attributes (uncovered bug during audit where `android:` prefix was missing in dictionary lookup).
- **`FX-31`**: Fixed height container (`250.dp`) with `verticalScroll(rememberScrollState())` demonstrating proper internal scrolling mitigation.
- **`FX-34`**: Fixed width sidebar menu (`280.dp`) with `fillMaxHeight()`, proving single-axis constraints are not flagged as dual-axis locked chains (`FF010`).
- **`FX-37`**: `LazyColumn` with fixed `300.dp` width and `400.dp` height, confirming internal lazy list scrolling prevents `FF010` dual-axis false positives.

---

## 3. Measured v0.9.2 Benchmark Results

Running the expanded 40-fixture suite in `AccuracyBenchmarkTest.kt`:

```text
==========================================
VIEWLINT v0.9.2 ACCURACY BENCHMARK SUMMARY
Total Fixtures Evaluated: 40
True Positives (TP): 16
True Negatives (TN): 24
False Positives (FP): 0
False Negatives (FN): 0
Measured Precision: 100.00%
Measured Recall: 100.00%
==========================================
```

### Verification Note & Limitation Warning
> [!IMPORTANT]
> While the expanded benchmark suite achieves 100% Precision and 100% Recall across all 40 controlled fixtures, **synthetic benchmark accuracy must never be conflated with real-world project accuracy**. Real-world Kotlin codebases contain complex expression trees, custom modifier extensions, dynamic configuration states, and indirect layout abstractions that lie outside regex-based static text heuristics. See `CROSS_PROJECT_VALIDATION.md` for real-world empirical findings.
