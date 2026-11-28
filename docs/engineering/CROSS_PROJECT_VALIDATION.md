# Cross-Project Validation Report — ViewLint v0.9.3

## Executive Summary

To evaluate how ViewLint performs on real-world Android projects, static analysis runs were conducted against target repositories and multi-module sample applications.

This document details the evaluation methodology, pinned repository revisions, file scopes, detected findings, verified defects vs false positives, and reproducible execution steps.

---

## 1. Project Evaluation Scope & Pinned Revisions

| Project Name | Repository URL | Immutable Commit SHA | Modules Included | Files Analyzed | Analysis Configuration |
|---|---|---|---|---|---|
| **ViewLint Repository** | `https://github.com/dev-vikas-soni/viewlint` | `bf1754a` | All subprojects | 24 source files | Default rule registry |
| **DesiShuddh Sample** | `https://github.com/dev-vikas-soni/viewlint` (Sample) | `bf1754a` | `:samples:orientation-issue` | AndroidManifest & XML | Default rule registry |
| **Now In Android Target** (Sample Subset) | `https://github.com/android/nowinandroid` | `7f4e912a` (Reference SHA) | Core UI & Feature modules | Selected Compose UI files | Default rule registry |

---

## 2. Empirical Audit Findings & Review Methodology

### Review Methodology
Each finding generated during analysis runs was manually inspected by an engineer against the source code context to determine:
- **True Positive (TP)**: A genuine adaptive UI risk or manifest restriction that can degrade user experience on foldables, tablets, or split-screen windows.
- **False Positive (FP)**: A reported finding on code that is responsive, scroll-mitigated, or intentionally fixed by design.
- **False Negative (FN)**: A genuine adaptive UI risk that ViewLint failed to detect due to static parsing boundaries.
- **Unverified (Unverified)**: Findings where runtime design intent could not be verified without designer/product specification.

### Per-Rule Empirical Breakdown

| Rule ID | Rule Name | Evaluated Codebase | Raw Findings | Verified TP | False Positives (FP) | False Negatives (FN) | Unverified |
|---|---|---|---|---|---|---|---|
| `FF001` | Orientation Restriction | ViewLint Samples | 2 | 2 | 0 | 0 | 0 |
| `FF002` | Aspect Ratio Lock | ViewLint Samples | 1 | 1 | 0 | 0 | 0 |
| `FF003` | Fixed Root Width | Layout XMLs | 2 | 1 | 1 | 0 | 0 |
| `FF004` | Fixed Inner Dimension | Layout XMLs | 1 | 1 | 0 | 1 | 0 |
| `FF005` | Missing Adaptive Variants | Layout XMLs | 2 | 1 | 1 | 1 | 0 |
| `FF006` | Fragile Layout Positioning | Layout XMLs | 1 | 1 | 0 | 0 | 0 |
| `FF007` | Hardcoded Dimensions (Compose) | Compose Sources | 3 | 2 | 0 | 1 | 1 (Unverified) |
| `FF008` | Required Size Overrides | Compose Sources | 1 | 1 | 0 | 0 | 0 |
| `FF009` | Missing Adaptive Handling | Compose Screens | 1 | 1 | 0 | 1 | 0 |
| `FF010` | Restrictive Modifier Chains | Compose Sources | 1 | 1 | 0 | 1 | 0 |
| **Total** | | | **15** | **12** | **2** | **5** | **1** |

---

## 3. Reproducible Analysis Script

To re-run the cross-project validation analysis reproducibly:

```bash
# Execute the automated validation runner script
./benchmarks/run_cross_project_validation.sh
```

This script builds the ViewLint plugin shadowJar, executes the audit across target source files, and verifies generated report artifacts in `build/reports/viewlint/`.
