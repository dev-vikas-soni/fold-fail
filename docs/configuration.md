# FoldFail Configuration Guide

FoldFail is configured via the `foldFail` extension in your Gradle build files.

---

## Extension Properties & Defaults

| Property | Type | Default Value | Description |
| :--- | :--- | :--- | :--- |
| `enabled` | `Boolean` | `true` | Enables or disables FoldFail analysis. |
| `runOnCheck` | `Boolean` | `true` | Automatically hooks FoldFail into the Gradle verification lifecycle (`./gradlew check`). |
| `reportFormats` | `List<String>` | `listOf("console")` | Report formats to generate (`console`, `json`, `html`, `sarif`). |
| `outputDir` | `Any` | `"build/reports/foldfail"` | Destination directory for exported reports. |
| `failOn` | `String` | `"NEVER"` | Quality gate threshold (`NEVER`, `ERROR`, `WARNING`, `NEW`). |
| `baselineFile` | `Any?` | `null` | Optional baseline file for tracking tech debt and filtering known findings. |

---

## Example Configuration

```kotlin
plugins {
    id("io.github.dev-vikas-soni.foldfail") version "0.7.0"
}

foldFail {
    enabled = true
    runOnCheck = true
    reportFormats = listOf("console", "json", "html", "sarif")
    outputDir = layout.buildDirectory.dir("reports/foldfail").get().asFile
    failOn = "WARNING"
    baselineFile = file("foldfail-baseline.txt")
}
```

---

## Standalone Task Execution

You can run the audit task independently of `check`:
```bash
./gradlew foldFail
```

---

## Gradle Verification Lifecycle Integration

When `enabled = true` and `runOnCheck = true`, applying the plugin automatically wires `foldFail` as a dependency of the project's `check` task during `afterEvaluate`. Running `./gradlew check` will execute all audits and enforce configured quality gates.
