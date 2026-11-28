# CI/CD Integration Guide

Integrate ViewLint into GitHub Actions, GitLab CI, or Bitbucket Pipelines.

## GitHub Actions Integration

```yaml
name: ViewLint Adaptive UI Audit

on:
  push:
    branches: [ main ]
  pull_request:
    branches: [ main ]

jobs:
  audit:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'

      - name: Run ViewLint Audit
        run: ./gradlew viewlint

      - name: Upload SARIF Report to GitHub Security Tab
        uses: github/codeql-action/upload-sarif@v3
        if: always()
        with:
          sarif_file: build/reports/viewlint/viewlint-report.sarif
```
