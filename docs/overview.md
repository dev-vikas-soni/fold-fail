# Viewlint Overview

## Vision & Scope

**Viewlint** is an open-source Android adaptive quality auditor. Its core vision is to empower Android developers and engineering teams to discover multi-window, foldable, tablet, and adaptive UI compatibility risks early in the development lifecycle via static code analysis, before runtime testing or production deployment.

## The Adaptive Android Problem

As the Android ecosystem expands across diverse form factors—including foldable devices, dual screens, tablets, Chromebooks, split-screen windowing, and desktop operating environments—applications are expected to handle arbitrary window sizes, resizing, and orientation changes gracefully.

Common technical pitfalls include:
- Explicit activity orientation locks (`screenOrientation="portrait"`).
- Rigid aspect ratio restrictions (`maxAspectRatio`).
- Hardcoded layout dimensions in XML resources and Compose modifiers (`Modifier.width(400.dp)`).
- Lack of qualified alternative layout variants (`-sw600dp`, `-land`).

These constraints often result in letterboxing, pillarboxing, clipped views, or broken multi-window experiences. Viewlint automates the detection of these patterns.

## Supported Project Types & Analysis Surfaces

Viewlint supports standard multi-module and single-module Android Gradle projects containing:
1. **Android Manifests** (`AndroidManifest.xml`): Scanned for activity restrictions.
2. **XML Layout Resources** (`res/layout*`): Scanned for fixed dimensions, absolute positioning, and structure complexity.
3. **Jetpack Compose Kotlin Sources** (`.kt`): Scanned for hardcoded size modifiers, required sizes, and missing adaptive handling cues.

## Static Analysis vs. Runtime Verification

Viewlint relies on **static code analysis** (manifest inspection, DOM XML parsing, and Kotlin source scanning). 
- **Strengths**: Instant feedback, deterministic execution, integration into Gradle verification (`./gradlew check`), and CI/CD code scanning pipelines.
- **Boundaries**: Static analysis cannot prove runtime layout correctness, dynamic state flows, or custom view measurement behavior. It serves as an automated quality gate rather than a substitute for runtime device testing.

## Intended Users & Use Cases
- **Android Engineers**: Catching hardcoded layout assumptions and orientation restrictions during code authoring and local builds (`./gradlew check`).
- **Platform & Architecture Leads**: Enforcing quality gates (`failOn = "WARNING"` or `"ERROR"`) and baselines (`viewlint-baseline.txt`) across enterprise codebases.
- **CI/CD Pipelines**: Publishing SARIF 2.1.0 security events directly to GitHub Code Scanning.
