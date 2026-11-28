# Reporting Capabilities

ViewLint generates four distinct report formats:

## 1. Console Output
Direct terminal output with color-coded severity badges, confidence ratings, exact file locations, and remediation suggestions.

## 2. Interactive HTML Report (`viewlint-report.html`)
- **Self-contained**: Zero external script or CSS dependencies (100% offline rendering).
- **Light/Dark theme**: Adapts automatically to system preferences.
- **Search & Filtering**: Filter findings by rule ID, severity, file path, or keyword in real-time.

## 3. JSON Report (`viewlint-report.json`)
Type-safe machine-readable report containing metadata, summary counts by rule, and structured findings.

## 4. SARIF 2.1.0 Report (`viewlint-report.sarif`)
Fully compliant with OASIS SARIF 2.1.0 schema for seamless integration into GitHub Code Scanning security tabs and GitLab SAST.
