#!/usr/bin/env bash
set -euo pipefail

# ViewLint v1.0.0 — Cross-Project Validation Reproducibility Script
# This script executes ViewLint static analysis across sample target modules
# and generates raw JSON, HTML, SARIF, and Console reports for evidence verification.

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

echo "========================================================================="
echo "ViewLint v1.0.0 — Reproducible Cross-Project Validation Execution"
echo "========================================================================="
echo "Project Root: ${PROJECT_ROOT}"
echo "Execution Time: $(date -u +"%Y-%m-%dT%H:%M:%SZ")"
echo "JDK Version: $(java -version 2>&1 | head -n 1)"
echo "-------------------------------------------------------------------------"

cd "${PROJECT_ROOT}"

# Build ShadowJar for plugin artifact
echo "[1/3] Building ViewLint Gradle Plugin ShadowJar..."
./gradlew :viewlint-gradle-plugin:shadowJar --quiet

# Execute ViewLint task
echo "[2/3] Executing ViewLint Analysis Engine across project sources..."
./gradlew viewlint --quiet || true

# Validate report artifacts
REPORT_DIR="${PROJECT_ROOT}/build/reports/viewlint"
echo "[3/3] Verifying Generated Report Artifacts..."

if [ -f "${REPORT_DIR}/viewlint-report.json" ]; then
    echo "  - JSON Report:  ${REPORT_DIR}/viewlint-report.json (FOUND)"
else
    echo "  - JSON Report:  MISSING"
    exit 1
fi

if [ -f "${REPORT_DIR}/viewlint-report.html" ]; then
    echo "  - HTML Report:  ${REPORT_DIR}/viewlint-report.html (FOUND)"
else
    echo "  - HTML Report:  MISSING"
    exit 1
fi

if [ -f "${REPORT_DIR}/viewlint-report.sarif" ]; then
    echo "  - SARIF Report: ${REPORT_DIR}/viewlint-report.sarif (FOUND)"
else
    echo "  - SARIF Report: MISSING"
    exit 1
fi

echo "========================================================================="
echo "Cross-Project Validation Completed Successfully."
echo "Raw findings report directory: ${REPORT_DIR}"
echo "========================================================================="
