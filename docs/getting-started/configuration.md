# Configuration Reference

Configure ViewLint via the `viewlint { ... }` block in your `build.gradle.kts`.

## Configuration Options

```kotlin
viewlint {
    // Enable or disable ViewLint task execution (default: true)
    enabled = true

    // Automatically attach viewlint task to Gradle 'check' lifecycle task (default: true)
    runOnCheck = true

    // List of output report formats. Options: "console", "json", "html", "sarif" (default: all)
    reportFormats = listOf("console", "json", "html", "sarif")

    // Output directory for report files (default: "build/reports/viewlint")
    outputDir = "build/reports/viewlint"

    // Quality gate threshold. Options: "NEVER", "ERROR", "WARNING", "NEW" (default: "NEVER")
    failOn = "WARNING"

    // Optional baseline file path to suppress existing findings
    baselineFile = file("viewlint-baseline.txt")
}
```

## Quality Gate Modes (`failOn`)
- `"NEVER"`: Run analysis, write reports, and exit with status 0 even if findings exist.
- `"ERROR"`: Fail the build if any `ERROR` severity findings exist.
- `"WARNING"`: Fail the build if any `WARNING` or `ERROR` severity findings exist.
- `"NEW"`: When `baselineFile` is set, fail the build if any new findings not present in the baseline are introduced.
