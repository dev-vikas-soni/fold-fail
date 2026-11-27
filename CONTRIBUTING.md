# Contributing to Viewlint

Thank you for your interest in contributing to Viewlint!

## Development Environment Setup
- **Java Development Kit**: JDK 21 or JDK 25.
- **Gradle**: Gradle 9.5.0 (via included `./gradlew wrapper`).

## Build and Test Commands
To build the project and execute all unit and functional tests:
```bash
./gradlew clean test build
```

## Module Responsibilities
- `:viewlint-core`: Data models (`ViewlintFinding`, `Severity`, `Confidence`), engine (`ViewlintEngine`), context (`ViewlintContext`), and reporting writers (`JsonReportWriter`, `HtmlReportWriter`, `SarifReportWriter`, `ViewlintBaseline`).
- `:viewlint-analyzer`: Manifest analyzer, XML layout parser & analyzers, and Compose analyzers/parsers.
- `:viewlint-gradle-plugin`: Gradle extension, task implementation, resource discovery, and lifecycle integration.

## Pull Request Checklist
1. Verify that all tests pass (`./gradlew clean test build`).
2. Add both positive and negative unit tests for any new rule or feature.
3. Ensure configuration-cache compatibility for any task changes.
4. Update documentation if configuration properties or behaviors change.
5. Do not modify rule semantics without explicit justification.
