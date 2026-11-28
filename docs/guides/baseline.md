# Baselines

Use baselines to introduce ViewLint into legacy codebases without failing builds on existing technical debt.

## How Baselines Work
1. When `baselineFile` points to a non-existent file path, ViewLint generates the file on the first run and records all current findings as signatures (`ruleId:file:evidence`).
2. On subsequent runs, ViewLint loads the baseline file and filters out baseline findings.
3. Only **new findings** introduced after baseline creation will trigger quality gate failures when `failOn = "NEW"` or `"WARNING"`.

## Configuration Example
```kotlin
viewlint {
    failOn = "NEW"
    baselineFile = file("config/viewlint-baseline.txt")
}
```

## Regenerating the Baseline
To update or regenerate the baseline file after fixing existing issues:
```bash
rm config/viewlint-baseline.txt
./gradlew viewlint
```
