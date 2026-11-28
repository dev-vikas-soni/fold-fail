# Kotlin Parsing Compatibility Matrix & Parser Boundary Specification

## Executive Summary

ViewLint utilizes a lightweight text-sanitization parser (`ComposeSourceParser.kt`) to extract composables and modifier function calls without invoking the full Kotlin compiler frontend.

This document details the exact syntax boundary, supported Kotlin constructs, known parsing edge cases, and the architectural limitations of static analysis.

---

## 1. Kotlin Syntax Construct Compatibility Matrix

| Kotlin Language Feature | Example Construct | Support Status | Parsing Behavior & Limitation |
|---|---|---|---|
| **Standard Modifier Chains** | `Modifier.fillMaxWidth().padding(16.dp)` | **Supported** | Cleanly tokenized dot-call sequence. |
| **Named Arguments** | `Modifier.width(width = 200.dp)` | **Supported** | Recognized via regex parameter matching. |
| **Range Sizing Modifiers** | `Modifier.widthIn(min = 100.dp, max = 300.dp)` | **Supported** | Recognized as flexible modifier, exempted from `FF007`. |
| **Single-Line Comments** | `// Modifier.width(300.dp)` | **Supported** | Stripped by `sanitizeKotlinSource`. |
| **Multi-Line Comments** | `/* Modifier.width(300.dp) */` | **Supported** | Stripped by `sanitizeKotlinSource`. |
| **Double-Quoted Strings** | `Text("Padding 16.dp")` | **Supported** | Sanitized out to prevent false modifier detection inside strings. |
| **Triple-Quoted Strings** | `"""val x = "16.dp""""` | **Partial** | Sanitizer treats inner `"` as string boundary. May leave trailing tokens. |
| **Character Literals** | `val c = '"'` or `val c = '{'` | **Partial** | Escaped quotes or braces in `Char` literals can corrupt brace counting. |
| **Nested Braces in Lambdas** | `Column { if(x) { Box { ... } } }` | **Supported** | Balanced brace depth tracking `braceCount`. |
| **Nested Function Calls** | `Modifier.padding(if(x) 8.dp else 16.dp)` | **Supported** | Parenthesis depth balance tracking `parenCount`. |
| **Modifier Variables** | `val m = Modifier.width(300.dp)` | **Limited** | Modifier tokenized within file, but disconnected from composable call site. |
| **Extension Modifiers** | `fun Modifier.mySize() = this.width(200.dp)` | **Limited** | Hardcoded dimension detected in extension function, but not at usage site. |
| **Multiline Modifiers** | `Modifier\n.width(200.dp)\n.height(300.dp)` | **Supported** | Sanitizer joins lines before dot-call extraction. |
| **Local / Member Composables**| `class Screen { @Composable fun Content() }` | **Supported** | Function scanner tracks `@Composable` annotations regardless of outer class. |

---

## 2. Fundamental Limitations of Static Analysis (PSI / FIR)

> [!IMPORTANT]
> **Static Analysis & PSI / FIR Boundaries**:
> While migrating to a full Kotlin Compiler PSI / FIR backend in future major versions will eliminate syntax parsing edge cases (such as multiline trailing lambdas or comment stripping), **PSI / FIR compiler resolution cannot guarantee zero false positives**.
>
> AST and symbol resolution can determine exact modifier types and variable reference chains, but **cannot establish whether a specific fixed dimension (e.g. `width(320.dp)`) represents an intentional design decision or an adaptive UI defect**. Static analysis provides defensible heuristics, but final adaptation decisions require human design intent.
