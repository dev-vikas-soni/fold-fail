# Benchmark Methodology & Accuracy Evaluation

## Evidence-Driven Benchmark Suite (`AccuracyBenchmarkTest.kt`)
To verify static analysis accuracy without over-promising production perfection, ViewLint maintains a **45-fixture evidence-driven benchmark suite**:
- **Fixtures**: `FX-01` through `FX-45` covering positive, negative, and edge cases across all 10 rules.
- **Assertions**: Validates exact Rule ID, Severity, Confidence, Evidence substring, and Line number.
- **Strict Accounting**: Asserts $19\text{ TP} + 26\text{ TN} = 45\text{ Total Fixtures}$.
- **Unpermitted Rule Check**: Fails the test if an unpermitted rule triggers on a fixture.

## Synthetic Benchmark Disclaimer
Synthetic benchmark performance (100% precision/recall on test fixtures) serves to verify internal engine logic and regression safety; it does not constitute proof of 100% accuracy on external real-world codebases.
