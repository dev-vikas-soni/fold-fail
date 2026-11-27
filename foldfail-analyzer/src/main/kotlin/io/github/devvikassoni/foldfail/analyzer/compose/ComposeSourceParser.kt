package io.github.devvikassoni.foldfail.analyzer.compose

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
                            braceCount++
                            foundOpeningBrace = true
                        } else if (char == '}') {
                            braceCount--
                        }
                    }
                    if (foundOpeningBrace && braceCount == 0) {
                        i = j + 1
                        break
                    }
                    if (j == lines.size - 1) {
                        i = lines.size
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
            } else {
                i++
            }
        }

        if (composables.isEmpty() && (text.contains("Modifier.") || text.contains("width") || text.contains("size"))) {
            val modifierCalls = extractModifierCalls(text)
            val hasAdaptive = hasAdaptiveCues(text)
            composables.add(
                ParsedComposable(
                    name = "FileRootComposable",
                    text = text,
                    modifierCalls = modifierCalls,
                    hasAdaptiveCues = hasAdaptive,
                    line = 1
                )
            )
        }

        return composables
    }

    private fun extractModifierCalls(text: String): List<String> {
        val keywords = listOf("requiredWidth", "requiredHeight", "requiredSize", "width", "height", "size")
        val calls = mutableListOf<String>()
        for (kw in keywords) {
            var index = text.indexOf(kw)
            while (index != -1) {
                val start = index
                val parenIndex = text.indexOf('(', start)
                if (parenIndex != -1 && parenIndex - start < 25) {
                    var depth = 0
                    var end = -1
                    for (k in parenIndex until text.length) {
                        if (text[k] == '(') depth++
                        else if (text[k] == ')') {
                            depth--
                            if (depth == 0) {
                                end = k + 1
                                break
                            }
                        }
                    }
                    if (end != -1) {
                        val call = text.substring(start, end)
                        if (!calls.contains(call)) {
                            calls.add(call)
                        }
                        index = text.indexOf(kw, end)
                    } else {
                        index = text.indexOf(kw, start + kw.length)
                    }
                } else {
                    index = text.indexOf(kw, start + kw.length)
                }
            }
        }
        return calls
    }

    private fun hasAdaptiveCues(text: String): Boolean {
        val adaptiveKeywords = listOf(
            "WindowSizeClass",
            "BoxWithConstraints",
            "WindowMetrics",
            "calculateWindowSizeClass",
            "isCompact",
            "isMedium",
            "isExpanded",
            "currentWindowAdaptiveInfo",
            "animateDpAsState",
            "when",
            "Orientation"
        )
        if (adaptiveKeywords.any { text.contains(it, ignoreCase = false) }) {
            return true
        }
        val hasConditionalBranch = text.contains("if (", ignoreCase = false) || text.contains("if(", ignoreCase = false)
        val hasSizingCheck = text.contains("maxWidth", ignoreCase = true) || text.contains("maxHeight", ignoreCase = true) || text.contains("screenWidth", ignoreCase = true)
        return hasConditionalBranch && hasSizingCheck
    }
}
