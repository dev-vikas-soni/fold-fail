package com.viewlint.analyzer.compose

import java.io.File

data class ParsedComposable(
    val name: String,
    val text: String,
    val modifierCalls: List<String>,
    val hasAdaptiveCues: Boolean,
    val line: Int
)

data class ParsedKotlinFile(
    val file: File,
    val composables: List<ParsedComposable>,
    val error: String? = null
)

object ComposeSourceParser {

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
            val line = lines[i].trim()
            if (line.contains("@Composable")) {
                var funcName = "ComposableFunction"
                val funcTextBuilder = StringBuilder()
                var braceCount = 0
                var foundOpeningBrace = false
                val startLine = i + 1

                for (j in i until lines.size) {
                    val l = lines[j]
                    funcTextBuilder.append(l).append("\n")

                    if (l.contains("fun ")) {
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

                val funcText = funcTextBuilder.toString()
                val modifierCalls = extractModifierCalls(funcText)
                val hasAdaptive = hasAdaptiveCues(funcText)

                composables.add(
                    ParsedComposable(
                        name = funcName,
                        text = funcText,
                        modifierCalls = modifierCalls,
                        hasAdaptiveCues = hasAdaptive,
                        line = startLine
                    )
                )
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
            lower.contains("displayfeature") ||
            lower.contains("foldingfeature")
    }
}
