# Gradle & Platform Compatibility Matrix

## Gradle Compatibility
- Supported: Gradle 8.0 through 9.x.
- Configuration-Cache: Tested and compliant via `@InputDirectory`, `@OutputDirectory`, and lazy Gradle properties.

## JDK / Java Runtime
- Tested on OpenJDK 21.
- Bytecode target: Java 21 (`JvmTarget.JVM_21`).

## Android Platform Compatibility
- Audits manifests and layouts targeting Android API 21 through API 36 (Android 16 Baklava).
- Accounts for Android 16 API 36 smallest-width 600dp orientation/aspect-ratio override behavior.
