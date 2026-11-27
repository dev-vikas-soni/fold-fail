# Reports & CI Integration Guide

FoldFail provides robust diagnostic export capabilities designed for local development and CI/CD pipelines.

---

## Report Formats

### 1. Console (`console`)
Prints formatted finding summaries, severity levels, file paths, evidence, and remediation directly to standard output.

### 2. JSON (`json`)
Exports structured data (`foldfail-report.json`) containing metadata, summary counters, rule distributions, and deterministically sorted findings with full JSON escaping.

### 3. HTML (`html`)
Generates a standalone responsive HTML report (`foldfail-report.html`) featuring summary statistic cards, severity badges, and a searchable findings table with zero external JavaScript dependencies.

### 4. SARIF 2.1.0 (`sarif`)
Produces a standards-compliant SARIF file (`foldfail-report.sarif`) containing driver metadata, rule definitions, relative artifact URIs, level mappings (`error`, `warning`, `note`), and `partialFingerprints` (`primaryLocationLineHash`) for stable finding identity across CI runs.

---

## Baselines & Quality Gates

### Baselines (`baselineFile`)
- On first run with a non-existent baseline file path, FoldFail automatically **creates** a baseline containing signatures (`ruleId:file:evidence`) of current findings.
- On subsequent runs, FoldFail **loads** the baseline, compares current findings, and isolates **new** findings.

### Quality Gates (`failOn`)
- `NEVER`: Never fail the build.
- `ERROR`: Fail build if any `ERROR` finding is present.
- `WARNING`: Fail build if any `WARNING` or `ERROR` finding is present.
- `NEW`: Fail build if any new finding exists relative to the baseline.

---

## GitHub Actions & Code Scanning Integration

FoldFail integrates natively with GitHub Code Scanning using the official SARIF upload action.

### Official Workflow Example (`.github/workflows/foldfail.yml`)
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

### Required Permissions & Limitations
- **Permissions**: Requires `security-events: write` to upload SARIF to GitHub Code Scanning.
- **Forks**: Fork PR workflows triggered via `pull_request` may have limited write permissions to security events depending on repository settings.
