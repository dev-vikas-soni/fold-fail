# Testing Guidelines

Run all unit, integration, and benchmark tests:

```bash
./gradlew test
```

Run accuracy benchmark:
```bash
./gradlew :viewlint-analyzer:test --tests com.viewlint.analyzer.benchmark.AccuracyBenchmarkTest
```

Run Gradle TestKit configuration-cache test:
```bash
./gradlew :viewlint-gradle-plugin:test --tests com.viewlint.gradle.ViewlintPluginTest
```
