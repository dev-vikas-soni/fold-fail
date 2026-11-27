package com.viewlint.analyzer.compose

import java.io.File

data class ParsedComposable(
    val name: String,
    val text: String,
    val modifierCalls: List<String>,
    val hasAdaptiveCues: Boolean,
    val isScreenOrContainer: Boolean,
    val isLeafOrSmallComponent: Boolean,
    val line: Int
)

data class ParsedKotlinFile(
    val file: File,
    val composables: List<ParsedComposable>,
    val error: String? = null
)

object ComposeSourceParser {

    private val screenOrContainerKeywords = listOf(
        "screen", "page", "content", "pane", "dashboard", "form", "layout", "container",
        "host", "scaffold", "feed", "view", "banner", "section", "card", "board", "dialog", "sheet"
    )

    private val leafOrSmallKeywords = listOf(
        "icon", "avatar", "badge", "divider", "spacer", "chip", "button", "tab",
        "indicator", "thumb", "handle", "logo", "bullet", "selector", "quantity", "price", "label", "counter"
    )

    fun parse(file: File): ParsedKotlinFile {
        if (!file.exists() || !file.isFile) {
            return ParsedKotlinFile(file, emptyList(), "File does not exist")
        }

        return try {
            val text = file.readText()
            val composables = extractComposables(text)
            ParsedKotlinFile(file, composables)
        } catch (e: Exception) {
            ParsedKotlinFile(file, emptyList(), e.message ?: "Parsing exception")
        }
    }

    private fun extractComposables(text: String): List<ParsedComposable> {
        val composables = mutableListOf<ParsedComposable>()
        val lines = text.lines()

        var i = 0
        while (i < lines.size) {
            val rawLine = lines[i]
            val line = rawLine.trim()

            // Ensure line is an actual @Composable annotation line, not inside a single-line comment
            if (line.startsWith("@Composable") || (line.contains("@Composable") && !line.startsWith("//") && !line.startsWith("/*"))) {
                var funcName: String? = null
                val funcTextBuilder = StringBuilder()
                var braceCount = 0
                var foundOpeningBrace = false
                val startLine = i + 1

                for (j in i until lines.size) {
                    val l = lines[j]
                    funcTextBuilder.append(l).append("\n")

                    if (funcName == null && l.contains("fun ")) {
                        val match = Regex("fun\\s+([a-zA-Z0-9_]+)").find(l)
                        if (match != null) {
                            funcName = match.groupValues[1]
                        }
                    }

                    for (char in l) {
                        if (char == '{') {
                            if (!foundOpeningBrace) foundOpeningBrace = true
                            braceCount++
                        } else if (char == '}') {
                            braceCount--
                        }
                    }

                    if (foundOpeningBrace && braceCount == 0) {
                        i = j
                        break
                    }
                }

                if (funcName != null) {
                    val funcText = funcTextBuilder.toString()
                    val modifierCalls = extractModifierCalls(funcText)
                    val hasAdaptive = hasAdaptiveCues(funcText)
                    val isContainer = isContainerComponent(funcName, funcText)
                    val isLeaf = isLeafComponent(funcName, funcText)

                    composables.add(
                        ParsedComposable(
                            name = funcName,
                            text = funcText,
                            modifierCalls = modifierCalls,
                            hasAdaptiveCues = hasAdaptive,
                            isScreenOrContainer = isContainer,
                            isLeafOrSmallComponent = isLeaf,
                            line = startLine
                        )
                    )
                }
            }
            i++
        }

        return composables
    }

    private fun extractModifierCalls(funcText: String): List<String> {
        val calls = mutableListOf<String>()
        val regex = Regex("\\.([a-zA-Z0-9_]+\\(.*?\\))")
        val matches = regex.findAll(funcText)
        for (match in matches) {
            calls.add(match.groupValues[1])
        }
        return calls
    }

    private fun hasAdaptiveCues(funcText: String): Boolean {
        val lower = funcText.lowercase()
        return lower.contains("windowsizeclass") ||
            lower.contains("boxwithconstraints") ||
            lower.contains("calculatewindowsizeclass") ||
            lower.contains("config.screenwidthdp") ||
            lower.contains("localconfiguration.current") ||
            lower.contains("displayfeature") ||
            lower.contains("foldingfeature") ||
            lower.contains("flowrow") ||
            lower.contains("flowcolumn") ||
            lower.contains("navigationsuitescaffold") ||
            lower.contains("navigationrail")
    }

    private fun isContainerComponent(funcName: String, funcText: String): Boolean {
        val nameLower = funcName.lowercase()
        val textLower = funcText.lowercase()

        val nameMatches = screenOrContainerKeywords.any { nameLower.contains(it) }
        val textMatches = textLower.contains("scaffold") ||
            textLower.contains("navhost") ||
            textLower.contains("lazycolumn") ||
            textLower.contains("lazyrow") ||
            textLower.contains("lazyverticalgrid")

        return nameMatches || textMatches
    }

    private fun isLeafComponent(funcName: String, funcText: String): Boolean {
        val nameLower = funcName.lowercase()
        val textLower = funcText.lowercase()
        val nameMatches = leafOrSmallKeywords.any { nameLower.contains(it) }
        val textMatches = textLower.contains("icon(") || textLower.contains("divider(") || textLower.contains("spacer(")
        return nameMatches || textMatches
    }
}
