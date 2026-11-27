package com.viewlint.core.reporting.html

import com.viewlint.core.reporting.ViewlintReport

object HtmlReportWriter {

    fun write(report: ViewlintReport): String {
        val sb = StringBuilder()
        sb.append("<!DOCTYPE html>\n")
        sb.append("<html lang=\"en\">\n<head>\n")
        sb.append("<meta charset=\"UTF-8\">\n")
        sb.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n")
        sb.append("<title>Viewlint Report — ${escapeHtml(report.metadata.projectName)}</title>\n")
        sb.append("<style>\n")
        sb.append("body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; margin: 0; padding: 20px; background: #f6f8fa; color: #24292e; }\n")
        sb.append(".container { max-width: 1200px; margin: auto; background: white; padding: 30px; border-radius: 8px; box-shadow: 0 1px 3px rgba(0,0,0,0.1); }\n")
        sb.append("h1 { color: #0366d6; margin-top: 0; }\n")
        sb.append(".metadata { background: #f1f8ff; padding: 15px; border-radius: 6px; margin-bottom: 20px; font-size: 14px; }\n")
        sb.append(".summary-cards { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 15px; margin-bottom: 30px; }\n")
        sb.append(".card { background: #fafbfc; border: 1px solid #e1e4e8; padding: 15px; border-radius: 6px; text-align: center; }\n")
        sb.append(".card h3 { margin: 0 0 10px 0; font-size: 16px; color: #586069; }\n")
        sb.append(".card .number { font-size: 24px; font-weight: bold; color: #24292e; }\n")
        sb.append("table { width: 100%; border-collapse: collapse; margin-top: 20px; }\n")
        sb.append("th, td { border: 1px solid #e1e4e8; padding: 12px; text-align: left; font-size: 14px; }\n")
        sb.append("th { background: #f6f8fa; font-weight: 600; }\n")
        sb.append(".badge { padding: 4px 8px; border-radius: 4px; font-size: 12px; font-weight: bold; color: white; }\n")
        sb.append(".badge-error { background: #d73a49; }\n")
        sb.append(".badge-warning { background: #ffab70; color: #24292e; }\n")
        sb.append(".badge-info { background: #0366d6; }\n")
        sb.append("code { background: rgba(27,31,35,0.05); padding: 2px 4px; border-radius: 3px; font-family: monospace; font-size: 13px; }\n")
        sb.append("</style>\n</head>\n<body>\n")
        sb.append("<div class=\"container\">\n")
        sb.append("<h1>Viewlint — Adaptive Quality Report</h1>\n")
        sb.append("<div class=\"metadata\">\n")
        sb.append("<strong>Project:</strong> ${escapeHtml(report.metadata.projectName)} | ")
        sb.append("<strong>Generated At:</strong> ${escapeHtml(report.metadata.generatedAt)} | ")
        sb.append("<strong>Tool Version:</strong> ${escapeHtml(report.metadata.toolVersion)}\n")
        sb.append("</div>\n")

        sb.append("<div class=\"summary-cards\">\n")
        sb.append("<div class=\"card\"><h3>Total Findings</h3><div class=\"number\">${report.summary.totalFindings}</div></div>\n")
        sb.append("<div class=\"card\"><h3>Errors</h3><div class=\"number\" style=\"color: #d73a49;\">${report.summary.errorCount}</div></div>\n")
        sb.append("<div class=\"card\"><h3>Warnings</h3><div class=\"number\" style=\"color: #b08800;\">${report.summary.warningCount}</div></div>\n")
        sb.append("<div class=\"card\"><h3>Info</h3><div class=\"number\" style=\"color: #0366d6;\">${report.summary.infoCount}</div></div>\n")
        sb.append("</div>\n")

        sb.append("<h2>Findings</h2>\n")
        if (report.findings.isEmpty()) {
            sb.append("<p>No findings detected. Your project looks adaptive and ready!</p>\n")
        } else {
            sb.append("<table>\n<thead>\n<tr>\n")
            sb.append("<th>Rule ID</th><th>Severity</th><th>Title</th><th>File</th><th>Evidence & Remediation</th>\n")
            sb.append("</tr>\n</thead>\n<tbody>\n")
            val sorted = report.findings.sortedWith(compareBy({ it.ruleId }, { it.file }))
            for (finding in sorted) {
                val badgeClass = when (finding.severity.name) {
                    "ERROR" -> "badge-error"
                    "WARNING" -> "badge-warning"
                    else -> "badge-info"
                }
                sb.append("<tr>\n")
                sb.append("<td><strong>${escapeHtml(finding.ruleId)}</strong></td>\n")
                sb.append("<td><span class=\"badge $badgeClass\">${finding.severity}</span></td>\n")
                sb.append("<td>${escapeHtml(finding.title)}<br><small style=\"color: #586069;\">${escapeHtml(finding.message)}</small></td>\n")
                sb.append("<td><code>${escapeHtml(finding.file)}</code></td>\n")
                sb.append("<td><strong>Evidence:</strong> <code>${escapeHtml(finding.evidence)}</code><br><br><strong>Remediation:</strong> ${escapeHtml(finding.remediation)}</td>\n")
                sb.append("</tr>\n")
            }
            sb.append("</tbody>\n</table>\n")
        }

        sb.append("</div>\n</body>\n</html>\n")
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
