# Viewlint Roadmap

Viewlint follows a structured engineering roadmap.

---

## Completed
- **v0.1 — Core Foundation**: Data models, severity/confidence levels, rule engine, and rule registry.
- **v0.2 — Manifest Analysis**: FF001 (Orientation restrictions) and FF002 (Aspect ratio restrictions), robust XML parsing, and manifest discovery.
- **v0.3 — XML Resource & Layout Analysis**: Layout resource discovery, secure DOM layout parsing, and rules FF003 through FF006.
- **v0.4 — Compose Analysis**: Kotlin source discovery, Compose source parser, and rules FF007 through FF010.
- **v0.5 — Reports, Baselines & Quality Gates**: Multi-format diagnostic exporters (`CONSOLE`, `JSON`, `HTML`, `SARIF`), baseline filtering, and configurable quality gates (`NEVER`, `ERROR`, `WARNING`, `NEW`).
- **v0.6 — GitHub Actions & SARIF Integration**: Official GitHub Actions workflow template and SARIF path normalization with partial fingerprints.
- **v0.7 — Gradle Verification Lifecycle Integration**: Automatic `./gradlew check` integration, extension configuration, and configuration-cache compatibility.
- **v0.7.1 — Rebrand & Self-Contained Publishing**: Complete rebrand to **Viewlint** (`com.viewlint`) and Shadow-packaged standalone plugin publishing.

---

## Implemented — Verification Pending
*(None)*

---

## Planned

### v0.8 — Custom Rule Authoring SPI
- Public custom rule API.
- External custom rule JAR support.
- Third-party rule examples.
- Classloader and configuration-cache validation.

---

## Under Consideration

### v0.9 — Runtime Adaptive Auditing
- ADB and emulator integration.
- Runtime configuration inspection.
- Resizable window testing.
- Foldable and tablet scenarios.
- Evidence collection.

### v0.10 — Static/Runtime Correlation
- Correlate static findings with runtime evidence.
- Reduce false positives.
- Real-world project benchmarks.
- Detection accuracy evaluation.

### v1.0 — Stable Release
- Stable API and compatibility policy.
- Documented supported environments.
- Reproducible validation.
- Release process.
- Contributor documentation.
- Public adoption readiness.
