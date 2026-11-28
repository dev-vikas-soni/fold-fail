# Quality Gates

ViewLint allows enforcing quality gates during CI/CD builds using the `failOn` extension setting.

## Quality Gate Options

| Option | Threshold | Typical Use Case |
| :--- | :--- | :--- |
| `"NEVER"` | Never fail | Informational monitoring in legacy projects |
| `"ERROR"` | Fail on `ERROR` severity | Basic safety gates (e.g., XML syntax failures) |
| `"WARNING"` | Fail on `WARNING` or `ERROR` | Strict quality enforcement for new projects |
| `"NEW"` | Fail on unbaselined findings | Incremental adoption in legacy codebases |

## CI Example
```kotlin
viewlint {
    enabled = true
    runOnCheck = true
    failOn = if (providers.environmentVariable("CI").isPresent) "WARNING" else "NEVER"
}
```
