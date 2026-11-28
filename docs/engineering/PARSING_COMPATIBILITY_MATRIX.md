# Kotlin Parsing Compatibility Matrix & Parser Boundary Specification

## Executive Summary

ViewLint currently utilizes a lightweight text-sanitization parser (`ComposeSourceParser.kt`) to extract composables and modifier function calls without invoking the full Kotlin compiler frontend.

This document documents the exact syntax boundary, supported Kotlin constructs, known parsing edge cases, and the architectural roadmap for Kotlin PSI / FIR integration in future major releases.

---

## 1. Kotlin Syntax Construct Compatibility Matrix

| Kotlin Language Feature | Example Construct | Support Status | Parsing Behavior / Limitation |
|---|---|---|---|
| **Standard Modifier Chains** | `Modifier.fillMaxWidth().padding(16.dp)` | **Supported** | Cleanly tokenized dot-call sequence. |
| **Named Arguments** | `Modifier.width(width = 200.dp)` | **Supported** | Extracted via regex `(width=200.dp)`. |
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

## 2. In-Depth Boundary Analysis of `ComposeSourceParser.kt`

### 1. Source Sanitization (`sanitizeKotlinSource`)
```kotlin
// Strips comments and string contents while maintaining line counts
private fun sanitizeKotlinSource(source: String): String
```
- **Supported**: Single line (`//`), block comments (`/* ... */`), double-quoted strings (`"..."`).
- **Edge Case**: Character literals like `val brace = '{'` are not currently parsed as character primitives. If a file contains standalone character braces in code logic, `braceCount` can miscalculate function boundaries.

### 2. Composable Extraction (`extractComposables`)
```kotlin
// Identifies @Composable functions and balances { } block scope
private fun extractComposables(sanitizedText: String): List<ParsedComposable>
```
- **Supported**: Standard `@Composable fun Name(...) { ... }` functions.
- **Edge Case**: Annotations on parameters or types (e.g. `val content: @Composable () -> Unit`) contain the `@Composable` string. The parser attempts to match `fun ` on subsequent lines.

### 3. Modifier Call Extraction (`extractModifierCalls`)
```kotlin
// Extracts dot-invocations on Modifier chains
private fun extractModifierCalls(funcText: String): List<String>
```
- **Supported**: Direct method chains (`Modifier.width(100.dp).height(200.dp)`).
- **Edge Case**: Variable-assigned modifiers passed through constructor parameters or composition locals cannot be resolved without type resolution.

---

## 3. PSI / FIR Compiler Integration Roadmap (ViewLint v1.0)

To transition ViewLint from text-heuristics to 100% semantic soundness on arbitrary Kotlin syntax, the v1.0 architecture will introduce a Kotlin Compiler PSI / FIR backend:

```mermaid
graph TD
    A[Kotlin Source Files] --> B{ViewLint Backend Engine}
    B -- Lightweight CI / Fast Mode --> C[ComposeSourceParser (Text Sanitizer)]
    B -- Full Semantic Analysis --> D[Kotlin Compiler PSI / FIR Parser]
    D --> E[Full AST & Symbol Table]
    E --> F[Type-Safe Modifier Resolution]
    E --> G[Cross-File Call Graph Traversal]
    C --> H[ViewLint Rule Engine]
    F --> H
    G --> H
```

### Benefits of Kotlin PSI Architecture:
1. **Zero Text False Positives**: Exact AST node verification replaces regex string matching.
2. **Variable & Extension Resolution**: Tracing modifier references across files and helper extension functions.
3. **Exact Line/Column Reporting**: Precise source code locations for SARIF and IDE integration.
