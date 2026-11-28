# Installation & Compatibility

## Plugin Coordinates
- **Plugin ID**: `io.github.dev-vikas-soni.viewlint`
- **Gradle Plugin Portal**: `https://plugins.gradle.org/plugin/io.github.dev-vikas-soni.viewlint`
- **Current Stable Version**: `1.0.0`

## Applying the Plugin

### Kotlin DSL (`build.gradle.kts`)
```kotlin
plugins {
    id("io.github.dev-vikas-soni.viewlint") version "1.0.0"
}
```

### Groovy DSL (`build.gradle`)
```groovy
plugins {
    id 'io.github.dev-vikas-soni.viewlint' version '1.0.0'
}
```

## Compatibility Requirements

| Requirement | Supported Range | Tested Version |
| :--- | :--- | :--- |
| **Gradle** | 8.0 – 9.x | 8.5, 9.5.0 |
| **Java / JDK** | 17, 21+ | OpenJDK 21 |
| **Kotlin** | 1.9.0 – 2.x | 2.0.21, 2.3.20 |
| **Android AGP** | 8.0 – 9.x | 8.2 – 9.0 |
