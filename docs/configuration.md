# Viewlint Configuration Guide

Viewlint is configured via the `viewlint` extension in your Gradle build files.

---

## Extension Properties & Defaults

| Property | Type | Default Value | Description |
| :--- | :--- | :--- | :--- |
| `enabled` | `Boolean` | `true` | Enables or disables Viewlint analysis. |
| `runOnCheck` | `Boolean` | `true` | Automatically hooks Viewlint into the Gradle verification lifecycle (`./gradlew check`). |
| `reportFormats` | `List<String>` | `listOf("console", "json", "html", "sarif")` | Report formats to generate (`console`, `json`, `html`, `sarif`). |
| `outputDir` | `Any` | `"build/reports/viewlint"` | Destination directory for exported reports. |
| `failOn` | `String` | `"NEVER"` | Quality gate threshold (`NEVER`, `ERROR`, `WARNING`, `NEW`). |
| `baselineFile` | `Any?` | `null` | Optional baseline file for tracking tech debt and filtering known findings. |

---

## Example Configuration

```kotlin
plugins {
    id("io.github.dev-vikas-soni.viewlint") version "0.8.0"
}

viewlint {
    enabled = true
    runOnCheck = true
    reportFormats = listOf("console", "json", "html", "sarif")
    outputDir = layout.buildDirectory.dir("reports/viewlint").get().asFile
    failOn = "WARNING"
    baselineFile = file("viewlint-baseline.txt")
}
```

---

## Standalone Task Execution

You can run the audit task independently of `check`:
```bash
./gradlew viewlint
```

---

## Gradle Verification Lifecycle Integration

When `enabled = true` and `runOnCheck = true`, applying the plugin automatically wires `viewlint` as a dependency of the project's `check` task during `afterEvaluate`. Running `./gradlew check` will execute all audits and enforce configured quality gates.
