# Contribution Guidelines

We welcome contributions to ViewLint!

## Guidelines
1. All new rules must include unit tests and corresponding benchmark fixtures in `AccuracyBenchmarkTest.kt`.
2. Do not break existing public configuration properties or report formats.
3. Keep dependency requirements light (zero runtime dependencies outside Kotlin compiler embeddable).
4. Run `./gradlew test` and verify clean build before submitting Pull Requests.
