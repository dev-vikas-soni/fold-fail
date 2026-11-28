# ViewLint Architecture & Engine Design

## Module Structure

ViewLint is organized into three decoupled Gradle modules:

```
viewlint/
├── viewlint-core            # Common data models, findings, baseline logic, and report writers
├── viewlint-analyzer       # XML & Jetpack Compose AST/regex parsers and rule implementations
└── viewlint-gradle-plugin  # Gradle plugin entry point, task configuration, and file discovery
```

## Static Analysis Flow

1. **File Discovery**: `ViewlintTask` scans project directory for `AndroidManifest.xml`, `res/layout/*.xml`, and `src/**/*.kt`.
2. **Context Assembly**: File paths and layout resources are passed to `ViewlintContext`.
3. **Rule Execution**: `ViewlintEngine` executes all registered `ViewlintRule` instances against `ViewlintContext`.
4. **Path Normalization**: Finding file paths are normalized to project-relative paths (`app/src/main/...`) for portability.
5. **Baseline Filtering**: `ViewlintBaseline` filters out known signatures if a baseline is configured.
6. **Report Generation**: `JsonReportWriter`, `HtmlReportWriter`, and `SarifReportWriter` emit output artifacts.
7. **Quality Gate Enforcement**: If threshold is exceeded, `GradleException` is thrown.
