package com.viewlint.core.reporting.html

import com.viewlint.core.reporting.ViewlintReport

object HtmlReportWriter {

    fun write(report: ViewlintReport): String {
        val total = report.summary.totalFindings
        val errors = report.summary.errorCount
        val warnings = report.summary.warningCount
        val infos = report.summary.infoCount

        val highConfCount = report.findings.count { it.confidence.name == "HIGH" }
        val medConfCount = report.findings.count { it.confidence.name == "MEDIUM" }
        val lowConfCount = report.findings.count { it.confidence.name == "LOW" }

        val criticalPrioCount = report.findings.count { it.priority.name == "CRITICAL" }
        val highPrioCount = report.findings.count { it.priority.name == "HIGH" }

        val manifestFindingsCount = report.findings.count { it.ruleId in setOf("FF001", "FF002") }
        val xmlFindingsCount = report.findings.count { it.ruleId in setOf("FF003", "FF004", "FF005", "FF006") }
        val composeFindingsCount = report.findings.count { it.ruleId in setOf("FF007", "FF008", "FF009", "FF010") }

        val overallRiskLabel = when {
            errors > 0 || criticalPrioCount > 0 -> "HIGH ADAPTIVE RISK"
            warnings > 0 || highPrioCount > 0 -> "MEDIUM ADAPTIVE RISK"
            else -> "LOW ADAPTIVE RISK"
        }

        val overallRiskClass = when {
            errors > 0 || criticalPrioCount > 0 -> "risk-high"
            warnings > 0 || highPrioCount > 0 -> "risk-medium"
            else -> "risk-low"
        }

        // Rank findings for Top Adaptive Risks
        val rankedFindings = report.findings.map { finding ->
            val prioScore = when (finding.priority.name) {
                "CRITICAL" -> 40
                "HIGH" -> 30
                "MEDIUM" -> 20
                else -> 10
            }
            val scopeScore = when (finding.ruleId) {
                "FF001", "FF002" -> 10
                "FF008", "FF010" -> 8
                "FF003", "FF007" -> 6
                else -> 4
            }
            val totalScore = prioScore + scopeScore
            Pair(finding, totalScore)
        }.sortedByDescending { it.second }

        val topRisks = rankedFindings.take(5)
        val findingsByFile = report.findings.groupBy { it.file }

        val sb = StringBuilder()
        sb.append("<!DOCTYPE html>\n")
        sb.append("<html lang=\"en\">\n<head>\n")
        sb.append("<meta charset=\"UTF-8\">\n")
        sb.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n")
        sb.append("<title>Viewlint Adaptive Quality Report — ${escapeHtml(report.metadata.projectName)}</title>\n")
        sb.append("<style>\n")
        sb.append("  :root {\n")
        sb.append("    --bg-page: #F8FAFC;\n")
        sb.append("    --bg-surface: #FFFFFF;\n")
        sb.append("    --bg-subtle: #F1F5F9;\n")
        sb.append("    --border-color: #E2E8F0;\n")
        sb.append("    --border-strong: #CBD5E1;\n")
        sb.append("    --text-primary: #0F172A;\n")
        sb.append("    --text-secondary: #475569;\n")
        sb.append("    --text-muted: #64748B;\n")
        sb.append("    --accent-blue: #2563EB;\n")
        sb.append("    --accent-blue-hover: #1D4ED8;\n")
        sb.append("    --accent-blue-light: #EFF6FF;\n")
        sb.append("    --error-red: #DC2626;\n")
        sb.append("    --error-bg: #FEE2E2;\n")
        sb.append("    --error-border: #FCA5A5;\n")
        sb.append("    --warning-yellow: #D97706;\n")
        sb.append("    --warning-bg: #FEF3C7;\n")
        sb.append("    --warning-border: #FCD34D;\n")
        sb.append("    --info-blue: #0284C7;\n")
        sb.append("    --info-bg: #E0F2FE;\n")
        sb.append("    --info-border: #BAE6FD;\n")
        sb.append("    --success-green: #059669;\n")
        sb.append("    --success-bg: #D1FAE5;\n")
        sb.append("    --shadow-sm: 0 1px 2px 0 rgba(0, 0, 0, 0.05);\n")
        sb.append("    --shadow-md: 0 4px 6px -1px rgba(0, 0, 0, 0.05), 0 2px 4px -1px rgba(0, 0, 0, 0.03);\n")
        sb.append("    --shadow-lg: 0 10px 15px -3px rgba(0, 0, 0, 0.08), 0 4px 6px -2px rgba(0, 0, 0, 0.04);\n")
        sb.append("  }\n")
        sb.append("  * { box-sizing: border-box; }\n")
        sb.append("  body {\n")
        sb.append("    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;\n")
        sb.append("    margin: 0; padding: 0; background: var(--bg-page); color: var(--text-primary); line-height: 1.5;\n")
        sb.append("    -webkit-font-smoothing: antialiased;\n")
        sb.append("  }\n")
        sb.append("  .header {\n")
        sb.append("    background: var(--bg-surface); border-bottom: 1px solid var(--border-color); padding: 20px 32px;\n")
        sb.append("    display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 16px;\n")
        sb.append("    box-shadow: var(--shadow-sm);\n")
        sb.append("  }\n")
        sb.append("  .brand { display: flex; align-items: center; gap: 14px; }\n")
        sb.append("  .brand-icon { width: 38px; height: 38px; background: var(--text-primary); border-radius: 10px; display: flex; align-items: center; justify-content: center; padding: 6px; }\n")
        sb.append("  .brand-title { font-size: 22px; font-weight: 800; color: var(--text-primary); margin: 0; letter-spacing: -0.5px; }\n")
        sb.append("  .brand-sub { font-size: 12px; font-weight: 700; color: var(--accent-blue); letter-spacing: 1.5px; text-transform: uppercase; }\n")
        sb.append("  .risk-badge { padding: 8px 16px; border-radius: 9999px; font-size: 12px; font-weight: 800; letter-spacing: 0.5px; text-transform: uppercase; }\n")
        sb.append("  .risk-high { background: var(--error-bg); color: var(--error-red); border: 1px solid var(--error-border); }\n")
        sb.append("  .risk-medium { background: var(--warning-bg); color: var(--warning-yellow); border: 1px solid var(--warning-border); }\n")
        sb.append("  .risk-low { background: var(--success-bg); color: var(--success-green); border: 1px solid #A7F3D0; }\n")
        sb.append("  .main-container { max-width: 1360px; margin: auto; padding: 32px 24px; }\n")
        sb.append("  .meta-bar {\n")
        sb.append("    background: var(--bg-surface); border: 1px solid var(--border-color); border-radius: 12px;\n")
        sb.append("    padding: 16px 24px; margin-bottom: 24px; display: flex; flex-wrap: wrap; gap: 24px;\n")
        sb.append("    font-size: 14px; color: var(--text-secondary); box-shadow: var(--shadow-sm);\n")
        sb.append("  }\n")
        sb.append("  .meta-item strong { color: var(--text-primary); }\n")
        sb.append("  .summary-grid {\n")
        sb.append("    display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 16px; margin-bottom: 24px;\n")
        sb.append("  }\n")
        sb.append("  .metric-card {\n")
        sb.append("    background: var(--bg-surface); border: 1px solid var(--border-color); border-radius: 12px; padding: 20px;\n")
        sb.append("    box-shadow: var(--shadow-sm); transition: transform 0.15s ease, box-shadow 0.15s ease;\n")
        sb.append("  }\n")
        sb.append("  .metric-card:hover { transform: translateY(-2px); box-shadow: var(--shadow-md); }\n")
        sb.append("  .metric-title { font-size: 12px; font-weight: 700; color: var(--text-muted); margin-bottom: 6px; text-transform: uppercase; letter-spacing: 0.5px; }\n")
        sb.append("  .metric-num { font-size: 30px; font-weight: 800; color: var(--text-primary); line-height: 1.1; }\n")
        sb.append("  .top-risks-card {\n")
        sb.append("    background: var(--bg-surface); border: 1px solid #93C5FD; border-radius: 12px; padding: 24px; margin-bottom: 24px;\n")
        sb.append("    box-shadow: var(--shadow-sm);\n")
        sb.append("  }\n")
        sb.append("  .top-risks-card h2 { font-size: 16px; margin: 0 0 16px 0; color: var(--accent-blue); display: flex; align-items: center; gap: 8px; font-weight: 700; }\n")
        sb.append("  .top-risk-item {\n")
        sb.append("    background: var(--bg-subtle); border: 1px solid var(--border-color); border-radius: 8px; padding: 14px 18px; margin-bottom: 10px;\n")
        sb.append("    display: flex; align-items: center; justify-content: space-between; gap: 16px; flex-wrap: wrap;\n")
        sb.append("  }\n")
        sb.append("  .top-risk-reason { font-size: 12px; color: var(--accent-blue); font-weight: 600; margin-top: 4px; }\n")
        sb.append("  .dist-grid {\n")
        sb.append("    display: grid; grid-template-columns: repeat(auto-fit, minmax(300px, 1fr)); gap: 16px; margin-bottom: 24px;\n")
        sb.append("  }\n")
        sb.append("  .dist-card {\n")
        sb.append("    background: var(--bg-surface); border: 1px solid var(--border-color); border-radius: 12px; padding: 20px; box-shadow: var(--shadow-sm);\n")
        sb.append("  }\n")
        sb.append("  .dist-card h3 { margin: 0 0 14px 0; font-size: 15px; color: var(--text-primary); border-bottom: 1px solid var(--border-color); padding-bottom: 10px; font-weight: 700; }\n")
        sb.append("  .bar-row { margin-bottom: 10px; }\n")
        sb.append("  .bar-label { display: flex; justify-content: space-between; font-size: 13px; color: var(--text-secondary); margin-bottom: 4px; font-weight: 500; }\n")
        sb.append("  .bar-track { height: 8px; background: var(--bg-subtle); border-radius: 4px; overflow: hidden; }\n")
        sb.append("  .bar-fill { height: 100%; border-radius: 4px; }\n")
        sb.append("  .file-summary-table { width: 100%; border-collapse: collapse; font-size: 13px; }\n")
        sb.append("  .file-summary-table th, .file-summary-table td { text-align: left; padding: 10px 14px; border-bottom: 1px solid var(--border-color); }\n")
        sb.append("  .file-summary-table th { color: var(--text-muted); font-weight: 600; background: var(--bg-subtle); }\n")
        sb.append("  .controls-bar {\n")
        sb.append("    position: sticky; top: 0; z-index: 100; background: var(--bg-surface); border: 1px solid var(--border-color);\n")
        sb.append("    border-radius: 12px; padding: 16px 20px; margin-bottom: 20px; display: flex; flex-wrap: wrap;\n")
        sb.append("    gap: 12px; align-items: center; justify-content: space-between; box-shadow: var(--shadow-md);\n")
        sb.append("  }\n")
        sb.append("  .search-input {\n")
        sb.append("    background: var(--bg-surface); border: 1px solid var(--border-strong); color: var(--text-primary);\n")
        sb.append("    padding: 9px 14px; border-radius: 8px; font-size: 14px; min-width: 260px; flex: 1; outline: none;\n")
        sb.append("    transition: border-color 0.15s ease, box-shadow 0.15s ease;\n")
        sb.append("  }\n")
        sb.append("  .search-input:focus { border-color: var(--accent-blue); box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.15); }\n")
        sb.append("  .filter-group { display: flex; gap: 6px; flex-wrap: wrap; align-items: center; }\n")
        sb.append("  .filter-btn {\n")
        sb.append("    background: var(--bg-surface); border: 1px solid var(--border-color); color: var(--text-secondary);\n")
        sb.append("    padding: 7px 12px; border-radius: 8px; font-size: 13px; font-weight: 600; cursor: pointer;\n")
        sb.append("    transition: all 0.15s ease;\n")
        sb.append("  }\n")
        sb.append("  .filter-btn:hover { border-color: var(--border-strong); color: var(--text-primary); background: var(--bg-subtle); }\n")
        sb.append("  .filter-btn.active { background: var(--accent-blue-light); border-color: var(--accent-blue); color: var(--accent-blue); }\n")
        sb.append("  .action-btn {\n")
        sb.append("    background: var(--bg-subtle); border: 1px solid var(--border-color); color: var(--text-primary); padding: 7px 12px;\n")
        sb.append("    border-radius: 8px; font-size: 13px; font-weight: 600; cursor: pointer; transition: background 0.15s ease;\n")
        sb.append("  }\n")
        sb.append("  .action-btn:hover { background: var(--border-color); }\n")
        sb.append("  .finding-card {\n")
        sb.append("    background: var(--bg-surface); border: 1px solid var(--border-color); border-radius: 12px;\n")
        sb.append("    margin-bottom: 12px; overflow: hidden; box-shadow: var(--shadow-sm); transition: border-color 0.15s ease, box-shadow 0.15s ease;\n")
        sb.append("  }\n")
        sb.append("  .finding-card:hover { border-color: var(--border-strong); box-shadow: var(--shadow-md); }\n")
        sb.append("  .finding-header {\n")
        sb.append("    padding: 16px 20px; cursor: pointer; display: flex; align-items: center; justify-content: space-between;\n")
        sb.append("    gap: 16px; user-select: none;\n")
        sb.append("  }\n")
        sb.append("  .finding-header:focus-visible { outline: 2px solid var(--accent-blue); outline-offset: -2px; }\n")
        sb.append("  .finding-title-group { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; flex: 1; }\n")
        sb.append("  .badge { padding: 3px 8px; border-radius: 6px; font-size: 11px; font-weight: 700; letter-spacing: 0.3px; text-transform: uppercase; }\n")
        sb.append("  .badge-ERROR { background: var(--error-bg); color: var(--error-red); border: 1px solid var(--error-border); }\n")
        sb.append("  .badge-WARNING { background: var(--warning-bg); color: var(--warning-yellow); border: 1px solid var(--warning-border); }\n")
        sb.append("  .badge-INFO { background: var(--info-bg); color: var(--info-blue); border: 1px solid var(--info-border); }\n")
        sb.append("  .badge-HIGH { background: var(--success-bg); color: var(--success-green); border: 1px solid #A7F3D0; }\n")
        sb.append("  .badge-MEDIUM { background: var(--warning-bg); color: var(--warning-yellow); border: 1px solid var(--warning-border); }\n")
        sb.append("  .badge-LOW { background: var(--bg-subtle); color: var(--text-muted); border: 1px solid var(--border-color); }\n")
        sb.append("  .badge-prio-CRITICAL { background: #7F1D1D; color: #FFFFFF; }\n")
        sb.append("  .badge-prio-HIGH { background: var(--error-bg); color: var(--error-red); border: 1px solid var(--error-border); }\n")
        sb.append("  .badge-prio-MEDIUM { background: var(--warning-bg); color: var(--warning-yellow); border: 1px solid var(--warning-border); }\n")
        sb.append("  .badge-prio-LOW { background: var(--bg-subtle); color: var(--text-muted); border: 1px solid var(--border-color); }\n")
        sb.append("  .finding-rule { font-family: monospace; font-size: 13px; font-weight: 700; color: var(--accent-blue); }\n")
        sb.append("  .finding-title { font-size: 15px; font-weight: 700; color: var(--text-primary); }\n")
        sb.append("  .finding-file { font-family: monospace; font-size: 12px; color: var(--text-secondary); opacity: 0.9; }\n")
        sb.append("  .arrow-icon { transition: transform 0.2s ease; font-size: 12px; color: var(--text-muted); }\n")
        sb.append("  .finding-card.open .arrow-icon { transform: rotate(180deg); }\n")
        sb.append("  .finding-body { display: none; padding: 20px 24px; border-top: 1px solid var(--border-color); background: #FAFDFD; }\n")
        sb.append("  .finding-card.open .finding-body { display: block; }\n")
        sb.append("  .detail-grid { display: grid; grid-template-columns: 2fr 1fr; gap: 24px; }\n")
        sb.append("  @media (max-width: 900px) { .detail-grid { grid-template-columns: 1fr; } }\n")
        sb.append("  .detail-section-title { font-size: 11px; font-weight: 700; text-transform: uppercase; color: var(--accent-blue); letter-spacing: 0.8px; margin-bottom: 6px; }\n")
        sb.append("  .detail-text { font-size: 14px; color: var(--text-primary); margin-bottom: 14px; line-height: 1.5; }\n")
        sb.append("  .code-block {\n")
        sb.append("    background: var(--bg-subtle); border: 1px solid var(--border-color); border-radius: 8px; padding: 12px 14px;\n")
        sb.append("    font-family: monospace; font-size: 13px; color: var(--text-primary); overflow-x: auto; margin-bottom: 14px;\n")
        sb.append("  }\n")
        sb.append("  .remediation-box {\n")
        sb.append("    background: var(--accent-blue-light); border: 1px solid #BFDBFE; border-radius: 8px; padding: 14px;\n")
        sb.append("    font-size: 13px; color: #1E40AF; margin-bottom: 14px; line-height: 1.5;\n")
        sb.append("  }\n")
        sb.append("  .side-card { background: var(--bg-surface); border: 1px solid var(--border-color); border-radius: 8px; padding: 16px; font-size: 13px; box-shadow: var(--shadow-sm); }\n")
        sb.append("  .empty-state { text-align: center; padding: 48px 24px; background: var(--bg-surface); border: 1px solid var(--border-color); border-radius: 12px; font-size: 15px; color: var(--text-secondary); box-shadow: var(--shadow-sm); }\n")
        sb.append("  .footer { margin-top: 48px; border-top: 1px solid var(--border-color); padding: 24px 0; text-align: center; font-size: 13px; color: var(--text-muted); }\n")
        sb.append("</style>\n</head>\n<body>\n")

        // Top Header
        sb.append("<div class=\"header\">\n")
        sb.append("  <div class=\"brand\">\n")
        sb.append("    <div class=\"brand-icon\">\n")
        sb.append("      <svg width=\"24\" height=\"24\" viewBox=\"0 0 512 512\" fill=\"none\">\n")
        sb.append("        <g stroke=\"#38BDF8\" stroke-width=\"28\" stroke-linecap=\"square\" fill=\"none\">\n")
        sb.append("          <path d=\"M 112 160 L 112 112 L 160 112\"/>\n")
        sb.append("          <path d=\"M 352 112 L 400 112 L 400 160\"/>\n")
        sb.append("          <path d=\"M 112 352 L 112 400 L 160 400\"/>\n")
        sb.append("          <path d=\"M 352 400 L 400 400 L 400 352\"/>\n")
        sb.append("        </g>\n")
        sb.append("        <path d=\"M 152 136 L 208 136 L 256 368 L 200 368 Z\" fill=\"#FFFFFF\"/>\n")
        sb.append("        <path d=\"M 304 136 L 360 136 L 296 232 L 248 232 Z\" fill=\"#38BDF8\"/>\n")
        sb.append("        <path d=\"M 280 268 L 336 268 L 256 368 L 200 368 L 232 320 Z\" fill=\"#38BDF8\"/>\n")
        sb.append("      </svg>\n")
        sb.append("    </div>\n")
        sb.append("    <div>\n")
        sb.append("      <div class=\"brand-title\">Viewlint</div>\n")
        sb.append("      <div class=\"brand-sub\">Adaptive Quality Report</div>\n")
        sb.append("    </div>\n")
        sb.append("  </div>\n")
        sb.append("  <div class=\"risk-badge $overallRiskClass\">$overallRiskLabel</div>\n")
        sb.append("</div>\n")

        // Main Container
        sb.append("<div class=\"main-container\">\n")

        // Metadata Bar
        sb.append("  <div class=\"meta-bar\">\n")
        sb.append("    <div class=\"meta-item\">Project: <strong>${escapeHtml(report.metadata.projectName)}</strong></div>\n")
        sb.append("    <div class=\"meta-item\">Generated: <strong>${escapeHtml(report.metadata.generatedAt)}</strong></div>\n")
        sb.append("    <div class=\"meta-item\">Tool Version: <strong>Viewlint v${escapeHtml(report.metadata.toolVersion)}</strong></div>\n")
        sb.append("  </div>\n")

        // Summary Metric Cards
        sb.append("  <div class=\"summary-grid\">\n")
        sb.append("    <div class=\"metric-card\"><div class=\"metric-title\">Total Findings</div><div class=\"metric-num\">$total</div></div>\n")
        sb.append("    <div class=\"metric-card\"><div class=\"metric-title\">Errors</div><div class=\"metric-num\" style=\"color: var(--error-red);\">$errors</div></div>\n")
        sb.append("    <div class=\"metric-card\"><div class=\"metric-title\">Warnings</div><div class=\"metric-num\" style=\"color: var(--warning-yellow);\">$warnings</div></div>\n")
        sb.append("    <div class=\"metric-card\"><div class=\"metric-title\">Informational</div><div class=\"metric-num\" style=\"color: var(--info-blue);\">$infos</div></div>\n")
        sb.append("    <div class=\"metric-card\"><div class=\"metric-title\">Rules Executed</div><div class=\"metric-num\" style=\"color: var(--accent-blue);\">10</div></div>\n")
        sb.append("  </div>\n")

        // Top Adaptive Risks Section
        if (topRisks.isNotEmpty()) {
            sb.append("  <div class=\"top-risks-card\">\n")
            sb.append("    <h2>⚡ Top Adaptive Risks (Prioritized Investigation)</h2>\n")
            for ((finding, score) in topRisks) {
                val reason = when {
                    finding.ruleId in setOf("FF001", "FF002") -> "Explicit manifest activity restriction impacting multi-window / foldable orientation"
                    finding.ruleId in setOf("FF008", "FF010") -> "Overriding required dimensions or restrictive modifier chain locking both layout axes"
                    finding.ruleId == "FF007" && finding.confidence.name == "HIGH" -> "Large hardcoded container dimension (>120dp) on unadapted layout region"
                    else -> "High-severity or high-confidence static adaptive UI risk"
                }
                sb.append("    <div class=\"top-risk-item\">\n")
                sb.append("      <div>\n")
                sb.append("        <div><span class=\"badge badge-${finding.severity.name}\">${finding.severity.name}</span> <span class=\"badge badge-prio-${finding.priority.name}\">${finding.priority.name}</span> <span class=\"finding-rule\">${escapeHtml(finding.ruleId)}</span> <strong style=\"color: var(--text-primary);\">${escapeHtml(finding.title)}</strong></div>\n")
                sb.append("        <div style=\"font-size: 13px; color: var(--text-secondary); margin-top: 4px;\">${escapeHtml(finding.message)}</div>\n")
                sb.append("        <div class=\"top-risk-reason\">Priority Score: $score • $reason</div>\n")
                sb.append("      </div>\n")
                sb.append("      <div class=\"finding-file\">${escapeHtml(finding.file)}</div>\n")
                sb.append("    </div>\n")
            }
            sb.append("  </div>\n")
        }

        // Distribution Dashboards
        sb.append("  <div class=\"dist-grid\">\n")

        // Severity Distribution
        sb.append("    <div class=\"dist-card\">\n")
        sb.append("      <h3>Severity Distribution</h3>\n")
        val errPct = if (total > 0) (errors * 100) / total else 0
        val warnPct = if (total > 0) (warnings * 100) / total else 0
        val infoPct = if (total > 0) (infos * 100) / total else 0
        sb.append("      <div class=\"bar-row\"><div class=\"bar-label\"><span>Errors ($errors)</span><span>$errPct%</span></div><div class=\"bar-track\"><div class=\"bar-fill\" style=\"width: $errPct%; background: var(--error-red);\"></div></div></div>\n")
        sb.append("      <div class=\"bar-row\"><div class=\"bar-label\"><span>Warnings ($warnings)</span><span>$warnPct%</span></div><div class=\"bar-track\"><div class=\"bar-fill\" style=\"width: $warnPct%; background: var(--warning-yellow);\"></div></div></div>\n")
        sb.append("      <div class=\"bar-row\"><div class=\"bar-label\"><span>Informational ($infos)</span><span>$infoPct%</span></div><div class=\"bar-track\"><div class=\"bar-fill\" style=\"width: $infoPct%; background: var(--info-blue);\"></div></div></div>\n")
        sb.append("    </div>\n")

        // Analysis Surface Breakdown
        sb.append("    <div class=\"dist-card\">\n")
        sb.append("      <h3>Analysis Surface Breakdown</h3>\n")
        val manPct = if (total > 0) (manifestFindingsCount * 100) / total else 0
        val xmlPct = if (total > 0) (xmlFindingsCount * 100) / total else 0
        val cmpPct = if (total > 0) (composeFindingsCount * 100) / total else 0
        sb.append("      <div class=\"bar-row\"><div class=\"bar-label\"><span>Android Manifest (FF001-002)</span><span>$manifestFindingsCount ($manPct%)</span></div><div class=\"bar-track\"><div class=\"bar-fill\" style=\"width: $manPct%; background: #0891B2;\"></div></div></div>\n")
        sb.append("      <div class=\"bar-row\"><div class=\"bar-label\"><span>XML Layouts (FF003-006)</span><span>$xmlFindingsCount ($xmlPct%)</span></div><div class=\"bar-track\"><div class=\"bar-fill\" style=\"width: $xmlPct%; background: var(--accent-blue);\"></div></div></div>\n")
        sb.append("      <div class=\"bar-row\"><div class=\"bar-label\"><span>Jetpack Compose (FF007-010)</span><span>$composeFindingsCount ($cmpPct%)</span></div><div class=\"bar-track\"><div class=\"bar-fill\" style=\"width: $cmpPct%; background: #6366F1;\"></div></div></div>\n")
        sb.append("    </div>\n")

        // Confidence Distribution
        sb.append("    <div class=\"dist-card\">\n")
        sb.append("      <h3>Confidence Distribution</h3>\n")
        val highPct = if (total > 0) (highConfCount * 100) / total else 0
        val medPct = if (total > 0) (medConfCount * 100) / total else 0
        val lowPct = if (total > 0) (lowConfCount * 100) / total else 0
        sb.append("      <div class=\"bar-row\"><div class=\"bar-label\"><span>High Confidence</span><span>$highConfCount ($highPct%)</span></div><div class=\"bar-track\"><div class=\"bar-fill\" style=\"width: $highPct%; background: var(--success-green);\"></div></div></div>\n")
        sb.append("      <div class=\"bar-row\"><div class=\"bar-label\"><span>Medium Confidence</span><span>$medConfCount ($medPct%)</span></div><div class=\"bar-track\"><div class=\"bar-fill\" style=\"width: $medPct%; background: var(--warning-yellow);\"></div></div></div>\n")
        sb.append("      <div class=\"bar-row\"><div class=\"bar-label\"><span>Low Confidence</span><span>$lowConfCount ($lowPct%)</span></div><div class=\"bar-track\"><div class=\"bar-fill\" style=\"width: $lowPct%; background: var(--text-muted);\"></div></div></div>\n")
        sb.append("    </div>\n")

        sb.append("  </div>\n")

        // Screen / File Level Risk Summaries
        if (findingsByFile.isNotEmpty()) {
            sb.append("  <div class=\"dist-card\" style=\"margin-bottom: 24px;\">\n")
            sb.append("    <h3>Screen & File Risk Summaries</h3>\n")
            sb.append("    <table class=\"file-summary-table\">\n")
            sb.append("      <thead><tr><th>File Location</th><th>Total Risks</th><th>Primary Severity</th><th>Highest Priority</th><th>Primary Rule</th></tr></thead>\n")
            sb.append("      <tbody>\n")
            for ((file, list) in findingsByFile) {
                val highestSev = when {
                    list.any { it.severity.name == "ERROR" } -> "ERROR"
                    list.any { it.severity.name == "WARNING" } -> "WARNING"
                    else -> "INFO"
                }
                val highestPrio = when {
                    list.any { it.priority.name == "CRITICAL" } -> "CRITICAL"
                    list.any { it.priority.name == "HIGH" } -> "HIGH"
                    list.any { it.priority.name == "MEDIUM" } -> "MEDIUM"
                    else -> "LOW"
                }
                val primaryRule = list.first().ruleId
                sb.append("        <tr>\n")
                sb.append("          <td><code style=\"color: var(--accent-blue); font-weight: 600;\">${escapeHtml(file)}</code></td>\n")
                sb.append("          <td><strong>${list.size}</strong></td>\n")
                sb.append("          <td><span class=\"badge badge-$highestSev\">$highestSev</span></td>\n")
                sb.append("          <td><span class=\"badge badge-prio-$highestPrio\">$highestPrio</span></td>\n")
                sb.append("          <td><span class=\"finding-rule\">$primaryRule</span></td>\n")
                sb.append("        </tr>\n")
            }
            sb.append("      </tbody>\n")
            sb.append("    </table>\n")
            sb.append("  </div>\n")
        }

        // Interactive Controls Bar
        sb.append("  <div class=\"controls-bar\">\n")
        sb.append("    <input type=\"text\" id=\"searchInput\" class=\"search-input\" placeholder=\"Search rule ID, title, file path, evidence...\" oninput=\"applyFilters()\">\n")

        sb.append("    <div class=\"filter-group\">\n")
        sb.append("      <button class=\"filter-btn active\" data-filter=\"sev\" data-val=\"ALL\" onclick=\"setFilter('sev', 'ALL')\">All Sev ($total)</button>\n")
        if (errors > 0) sb.append("      <button class=\"filter-btn\" data-filter=\"sev\" data-val=\"ERROR\" onclick=\"setFilter('sev', 'ERROR')\">Error ($errors)</button>\n")
        if (warnings > 0) sb.append("      <button class=\"filter-btn\" data-filter=\"sev\" data-val=\"WARNING\" onclick=\"setFilter('sev', 'WARNING')\">Warning ($warnings)</button>\n")
        if (infos > 0) sb.append("      <button class=\"filter-btn\" data-filter=\"sev\" data-val=\"INFO\" onclick=\"setFilter('sev', 'INFO')\">Info ($infos)</button>\n")
        sb.append("    </div>\n")

        sb.append("    <div class=\"filter-group\">\n")
        sb.append("      <button class=\"filter-btn active\" data-filter=\"cat\" data-val=\"ALL\" onclick=\"setFilter('cat', 'ALL')\">All Surfaces</button>\n")
        if (manifestFindingsCount > 0) sb.append("      <button class=\"filter-btn\" data-filter=\"cat\" data-val=\"MANIFEST\" onclick=\"setFilter('cat', 'MANIFEST')\">Manifest ($manifestFindingsCount)</button>\n")
        if (xmlFindingsCount > 0) sb.append("      <button class=\"filter-btn\" data-filter=\"cat\" data-val=\"XML\" onclick=\"setFilter('cat', 'XML')\">XML ($xmlFindingsCount)</button>\n")
        if (composeFindingsCount > 0) sb.append("      <button class=\"filter-btn\" data-filter=\"cat\" data-val=\"COMPOSE\" onclick=\"setFilter('cat', 'COMPOSE')\">Compose ($composeFindingsCount)</button>\n")
        sb.append("    </div>\n")

        sb.append("    <div class=\"filter-group\">\n")
        sb.append("      <button class=\"action-btn\" onclick=\"toggleExpandAll(true)\">Expand All</button>\n")
        sb.append("      <button class=\"action-btn\" onclick=\"toggleExpandAll(false)\">Collapse All</button>\n")
        sb.append("      <button class=\"action-btn\" onclick=\"resetFilters()\">Reset</button>\n")
        sb.append("    </div>\n")
        sb.append("    <div id=\"matchingCount\" style=\"font-size: 13px; font-weight: 600; color: var(--text-muted); width: 100%; margin-top: 4px;\">Showing $total of $total findings</div>\n")
        sb.append("  </div>\n")

        // Findings List Container
        sb.append("  <div id=\"findingsContainer\">\n")

        if (report.findings.isEmpty()) {
            sb.append("    <div class=\"empty-state\">✨ No adaptive UI risks detected! Your layout configurations adapt fluidly across devices.</div>\n")
        } else {
            val sorted = report.findings.sortedWith(compareBy({ it.ruleId }, { it.file }))
            for ((index, finding) in sorted.withIndex()) {
                val cat = when (finding.ruleId) {
                    "FF001", "FF002" -> "MANIFEST"
                    "FF003", "FF004", "FF005", "FF006" -> "XML"
                    else -> "COMPOSE"
                }

                val adaptiveImpact = when (finding.ruleId) {
                    "FF001" -> "Explicit orientation locks cause letterboxing, pillarboxing, or broken window resizing when users rotate devices, fold/unfold foldables, or enter split-screen mode."
                    "FF002" -> "Fixed aspect-ratio limits prevent activities from scaling across wide foldable inner displays or multi-window splits."
                    "FF003" -> "Hardcoded root widths in XML layouts stop views from expanding to fill tablet or desktop window boundaries."
                    "FF004" -> "Large hardcoded dp bounds on non-scrolling views risk clipping or layout truncation on smaller or split window sizes."
                    "FF005" -> "Complex layout hierarchies lacking adaptive qualifiers (-w600dp, -land) miss opportunities for multi-pane tablet layouts."
                    "FF006" -> "Absolute positioning locks views to hardcoded X/Y coordinates, breaking view alignment when screen sizes change."
                    "FF007" -> "Fixed composable size modifiers constrain content panels across variable window sizes, risking clipped content."
                    "FF008" -> "Required size modifiers override parent incoming constraints, forcing exact bounds that ignore window boundaries."
                    "FF009" -> "Complex screen composables lacking WindowSizeClass or responsive branching may render unadapted single-column layouts on tablets."
                    "FF010" -> "Combining fixed width and height modifiers locks component boundaries in both dimensions, preventing responsive reflow."
                    else -> "Fixed layout constraints hinder responsive screen adaptation across varied Android form factors."
                }

                val confidenceExplanation = when (finding.confidence.name) {
                    "HIGH" -> "HIGH: Direct, explicit, or overriding layout constraint declared without adaptive handling."
                    "MEDIUM" -> "MEDIUM: Suspicious dimension or restriction pattern detected requiring developer review."
                    else -> "LOW: Structural heuristic suggestion for developer review."
                }

                val priorityExplanation = when (finding.priority.name) {
                    "CRITICAL" -> "CRITICAL: Urgent investigation required; high impact error on primary window boundaries."
                    "HIGH" -> "HIGH: High priority; direct constraint override or orientation lock."
                    "MEDIUM" -> "MEDIUM: Medium priority; potential dimension constraint on non-root component."
                    else -> "LOW: Low priority; structural suggestion or informational audit item."
                }

                val ruleCategoryDoc = when (finding.ruleId) {
                    "FF001" -> "Android Manifest Rule • Activity Orientation Lock"
                    "FF002" -> "Android Manifest Rule • Activity Aspect Ratio Constraint"
                    "FF003" -> "XML Layout Rule • Fixed Root Layout Width"
                    "FF004" -> "XML Layout Rule • Fixed Large Inner Bounds"
                    "FF005" -> "XML Layout Rule • Complex Layout Adaptive Qualifiers"
                    "FF006" -> "XML Layout Rule • Absolute Layout Positioning"
                    "FF007" -> "Jetpack Compose Rule • Fixed Dimension Modifier"
                    "FF008" -> "Jetpack Compose Rule • Restrictive Required Size Modifier"
                    "FF009" -> "Jetpack Compose Rule • Missing WindowSizeClass / Responsive Branching"
                    "FF010" -> "Jetpack Compose Rule • Restrictive Modifier Chain Combination"
                    else -> "Viewlint Adaptive Static Analysis Rule"
                }

                sb.append("    <div class=\"finding-card\" id=\"card-$index\" data-sev=\"${finding.severity.name}\" data-cat=\"$cat\" data-conf=\"${finding.confidence.name}\" data-prio=\"${finding.priority.name}\" data-rule=\"${finding.ruleId}\" data-search=\"${escapeHtml("${finding.ruleId} ${finding.title} ${finding.file} ${finding.evidence} ${finding.message}").lowercase()}\">\n")
                sb.append("      <div class=\"finding-header\" tabindex=\"0\" role=\"button\" aria-expanded=\"false\" aria-controls=\"body-$index\" onclick=\"toggleCard($index)\" onkeydown=\"if(event.key==='Enter'||event.key===' '){toggleCard($index);event.preventDefault();}\">\n")
                sb.append("        <div class=\"finding-title-group\">\n")
                sb.append("          <span class=\"badge badge-${finding.severity.name}\">${finding.severity.name}</span>\n")
                sb.append("          <span class=\"badge badge-prio-${finding.priority.name}\">${finding.priority.name} PRIORITY</span>\n")
                sb.append("          <span class=\"badge badge-${finding.confidence.name}\">${finding.confidence.name} CONFIDENCE</span>\n")
                sb.append("          <span class=\"finding-rule\">${escapeHtml(finding.ruleId)}</span>\n")
                sb.append("          <span class=\"finding-title\">${escapeHtml(finding.title)}</span>\n")
                sb.append("        </div>\n")
                sb.append("        <div class=\"finding-file\">${escapeHtml(finding.file)}</div>\n")
                sb.append("        <div class=\"arrow-icon\">▼</div>\n")
                sb.append("      </div>\n")

                sb.append("      <div class=\"finding-body\" id=\"body-$index\">\n")
                sb.append("        <div class=\"detail-grid\">\n")
                sb.append("          <div>\n")
                sb.append("            <div class=\"detail-section-title\">What Was Detected</div>\n")
                sb.append("            <div class=\"detail-text\">${escapeHtml(finding.message)}</div>\n")

                sb.append("            <div class=\"detail-section-title\">Source Evidence</div>\n")
                sb.append("            <div class=\"code-block\">${escapeHtml(finding.evidence)}</div>\n")

                sb.append("            <div class=\"detail-section-title\">Why This Affects Adaptive UI</div>\n")
                sb.append("            <div class=\"detail-text\">$adaptiveImpact</div>\n")

                sb.append("            <div class=\"detail-section-title\">Recommended Action</div>\n")
                sb.append("            <div class=\"remediation-box\">💡 <strong>Remediation:</strong> ${escapeHtml(finding.remediation)}</div>\n")
                sb.append("          </div>\n")

                sb.append("          <div>\n")
                sb.append("            <div class=\"side-card\">\n")
                sb.append("              <div class=\"detail-section-title\">Rule Context</div>\n")
                sb.append("              <div style=\"margin-bottom: 12px; font-weight: 600;\">$ruleCategoryDoc</div>\n")
                sb.append("              <div class=\"detail-section-title\">Priority Score</div>\n")
                sb.append("              <div style=\"margin-bottom: 12px;\"><strong>$priorityExplanation</strong></div>\n")
                sb.append("              <div class=\"detail-section-title\">Confidence Rating</div>\n")
                sb.append("              <div style=\"margin-bottom: 12px;\"><strong>$confidenceExplanation</strong></div>\n")
                sb.append("              <div class=\"detail-section-title\">File Location</div>\n")
                sb.append("              <div style=\"word-break: break-all; font-family: monospace; font-size: 12px;\">${escapeHtml(finding.file)}</div>\n")
                sb.append("            </div>\n")
                sb.append("          </div>\n")
                sb.append("        </div>\n")
                sb.append("      </div>\n")
                sb.append("    </div>\n")
            }
        }

        sb.append("    <div id=\"noMatchState\" class=\"empty-state\" style=\"display: none;\">🔍 No findings match your current search or filter selection. Click 'Reset' to view all findings.</div>\n")
        sb.append("  </div>\n")

        // Footer
        sb.append("  <div class=\"footer\">\n")
        sb.append("    <div><strong>Viewlint v${escapeHtml(report.metadata.toolVersion)}</strong> • Generated at ${escapeHtml(report.metadata.generatedAt)}</div>\n")
        sb.append("    <div style=\"margin-top: 6px; opacity: 0.8;\">Designed for Android Adaptive UI Quality Audit • Open Source on <a href=\"https://github.com/dev-vikas-soni/viewlint\" style=\"color: var(--accent-blue); text-decoration: none; font-weight: 600;\">GitHub</a></div>\n")
        sb.append("  </div>\n")
        sb.append("</div>\n")

        // Vanilla JavaScript Filter & Search Engine
        sb.append("<script>\n")
        sb.append("  let activeSev = 'ALL';\n")
        sb.append("  let activeCat = 'ALL';\n")
        sb.append("  const totalCount = $total;\n")

        sb.append("  function toggleCard(index) {\n")
        sb.append("    const card = document.getElementById('card-' + index);\n")
        sb.append("    const header = card ? card.querySelector('.finding-header') : null;\n")
        sb.append("    if (card) {\n")
        sb.append("      const isOpen = card.classList.toggle('open');\n")
        sb.append("      if (header) header.setAttribute('aria-expanded', isOpen ? 'true' : 'false');\n")
        sb.append("    }\n")
        sb.append("  }\n")

        sb.append("  function toggleExpandAll(expand) {\n")
        sb.append("    document.querySelectorAll('.finding-card').forEach(card => {\n")
        sb.append("      if (card.style.display !== 'none') {\n")
        sb.append("        if (expand) card.classList.add('open'); else card.classList.remove('open');\n")
        sb.append("        const header = card.querySelector('.finding-header');\n")
        sb.append("        if (header) header.setAttribute('aria-expanded', expand ? 'true' : 'false');\n")
        sb.append("      }\n")
        sb.append("    });\n")
        sb.append("  }\n")

        sb.append("  function setFilter(type, value) {\n")
        sb.append("    if (type === 'sev') activeSev = value;\n")
        sb.append("    if (type === 'cat') activeCat = value;\n")
        sb.append("    document.querySelectorAll('.filter-btn[data-filter=\"' + type + '\"]').forEach(btn => {\n")
        sb.append("      btn.classList.toggle('active', btn.getAttribute('data-val') === value);\n")
        sb.append("    });\n")
        sb.append("    applyFilters();\n")
        sb.append("  }\n")

        sb.append("  function resetFilters() {\n")
        sb.append("    document.getElementById('searchInput').value = '';\n")
        sb.append("    activeSev = 'ALL';\n")
        sb.append("    activeCat = 'ALL';\n")
        sb.append("    document.querySelectorAll('.filter-btn').forEach(btn => {\n")
        sb.append("      btn.classList.toggle('active', btn.getAttribute('data-val') === 'ALL');\n")
        sb.append("    });\n")
        sb.append("    applyFilters();\n")
        sb.append("  }\n")

        sb.append("  function applyFilters() {\n")
        sb.append("    const query = document.getElementById('searchInput').value.toLowerCase().trim();\n")
        sb.append("    const cards = document.querySelectorAll('.finding-card');\n")
        sb.append("    let visibleCount = 0;\n")

        sb.append("    cards.forEach(card => {\n")
        sb.append("      const sev = card.getAttribute('data-sev');\n")
        sb.append("      const cat = card.getAttribute('data-cat');\n")
        sb.append("      const search = card.getAttribute('data-search') || '';\n")

        sb.append("      const matchesSev = (activeSev === 'ALL' || sev === activeSev);\n")
        sb.append("      const matchesCat = (activeCat === 'ALL' || cat === activeCat);\n")
        sb.append("      const matchesQuery = (!query || search.includes(query));\n")

        sb.append("      if (matchesSev && matchesCat && matchesQuery) {\n")
        sb.append("        card.style.display = 'block';\n")
        sb.append("        visibleCount++;\n")
        sb.append("      } else {\n")
        sb.append("        card.style.display = 'none';\n")
        sb.append("      }\n")
        sb.append("    });\n")

        sb.append("    const counter = document.getElementById('matchingCount');\n")
        sb.append("    if (counter) counter.innerText = `Showing \${visibleCount} of \${totalCount} findings`;\n")

        sb.append("    const noMatchState = document.getElementById('noMatchState');\n")
        sb.append("    if (noMatchState) {\n")
        sb.append("      noMatchState.style.display = (visibleCount === 0 && cards.length > 0) ? 'block' : 'none';\n")
        sb.append("    }\n")
        sb.append("  }\n")
        sb.append("</script>\n")
        sb.append("</body>\n</html>\n")

        return sb.toString()
    }

    private fun escapeHtml(input: String): String {
        return input
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#x27;")
    }
}
