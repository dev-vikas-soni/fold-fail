package com.viewlint.analyzer.compose

import java.io.File

enum class ComponentRole {
    SCREEN_OR_CONTAINER,
    LEAF_OR_SMALL,
    INPUT_SURFACE,
    UNKNOWN
}

data class ParsedModifierChain(
    val rawText: String,
    val calls: List<String>,
    val hasScrollModifier: Boolean,
    val targetPrimitive: String? = null
)

data class ParsedComposable(
    val name: String,
    val text: String,
    val modifierChains: List<ParsedModifierChain>,
    val modifierCalls: List<String>,
    val role: ComponentRole,
    val hasAdaptiveCues: Boolean,
    val isScreenOrContainer: Boolean,
    val isLeafOrSmallComponent: Boolean,
    val isInputSurface: Boolean = false,
    val hasInternalScroll: Boolean = false,
    val hasTextContent: Boolean = false,
    val hasScrollableParent: Boolean = false,
    val line: Int
)

data class ParsedKotlinFile(
    val file: File,
    val composables: List<ParsedComposable>,
    val error: String? = null
)

object ComposeSourceParser {

    private val screenOrContainerKeywords = listOf(
        "screen", "page", "dashboard", "form", "scaffold", "navhost", "card", "sheet", "panel", "dialog", "container", "overview"
    )

    private val leafOrSmallKeywords = listOf(
        "icon", "avatar", "badge", "divider", "spacer", "chip", "button", "tab",
        "indicator", "thumb", "handle", "logo", "bullet", "selector", "quantity", "price",
        "label", "counter", "progress", "progressbar", "checkbox", "switch", "radio",
        "image", "thumbnail", "photo", "pic", "graphic", "illustration", "dot", "star", "rating",
        "field", "input", "otp", "textfield", "pin", "cell", "textinput", "character", "entry", "item"
    )

    fun parse(file: File): ParsedKotlinFile {
        if (!file.exists() || !file.isFile) {
            return ParsedKotlinFile(file, emptyList(), "File does not exist")
        }

        return try {
            val rawText = file.readText()
            val sanitizedText = sanitizeKotlinSource(rawText)
            val composables = extractComposables(sanitizedText)
            ParsedKotlinFile(file, composables)
        } catch (e: Exception) {
            ParsedKotlinFile(file, emptyList(), e.message ?: "Parsing exception")
        }
    }

    private fun sanitizeKotlinSource(source: String): String {
        val sb = StringBuilder()
        var i = 0
        val len = source.length
        var inSingleLineComment = false
        var inMultiLineComment = false
        var inString = false

        while (i < len) {
            val c = source[i]
            val next = if (i + 1 < len) source[i + 1] else ' '

            if (inSingleLineComment) {
                if (c == '\n') {
                    inSingleLineComment = false
                    sb.append(c)
                } else {
                    sb.append(' ')
                }
            } else if (inMultiLineComment) {
                if (c == '*' && next == '/') {
                    inMultiLineComment = false
                    sb.append("  ")
                    i++
                } else if (c == '\n') {
                    sb.append('\n')
                } else {
                    sb.append(' ')
                }
            } else if (inString) {
                if (c == '\\' && i + 1 < len) {
                    sb.append("  ")
                    i++
                } else if (c == '"') {
                    inString = false
                    sb.append('"')
                } else if (c == '\n') {
                    sb.append('\n')
                } else {
                    sb.append(' ')
                }
            } else {
                if (c == '/' && next == '/') {
                    inSingleLineComment = true
                    sb.append("  ")
                    i++
                } else if (c == '/' && next == '*') {
                    inMultiLineComment = true
                    sb.append("  ")
                    i++
                } else if (c == '"') {
                    inString = true
                    sb.append('"')
                } else {
                    sb.append(c)
                }
            }
            i++
        }
        return sb.toString()
    }

    private fun extractComposables(sanitizedText: String): List<ParsedComposable> {
        val composables = mutableListOf<ParsedComposable>()
        val lines = sanitizedText.lines()

        var i = 0
        while (i < lines.size) {
            val line = lines[i].trim()

            if (line.startsWith("@Composable") || line.contains("@Composable")) {
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
                    val chains = extractModifierChains(funcText)
                    val modifierCalls = chains.flatMap { it.calls }
                    val hasAdaptive = hasAdaptiveCues(funcName, funcText)
                    val isInput = isInputSurfaceComponent(funcName, funcText)
                    val isLeaf = isLeafComponent(funcName, funcText, isInput)
                    val isContainer = isContainerComponent(funcName, funcText)
                    val role = determineRole(isInput, isLeaf, isContainer, funcText)

                    val internalScroll = hasInternalScrollState(funcText)
                    val textContent = hasVariableTextContent(funcText)
                    val scrollParent = hasScrollableContainer(funcText)

                    composables.add(
                        ParsedComposable(
                            name = funcName,
                            text = funcText,
                            modifierChains = chains,
                            modifierCalls = modifierCalls,
                            role = role,
                            hasAdaptiveCues = hasAdaptive,
                            isScreenOrContainer = (role == ComponentRole.SCREEN_OR_CONTAINER),
                            isLeafOrSmallComponent = (role == ComponentRole.LEAF_OR_SMALL),
                            isInputSurface = (role == ComponentRole.INPUT_SURFACE),
                            hasInternalScroll = internalScroll,
                            hasTextContent = textContent,
                            hasScrollableParent = scrollParent,
                            line = startLine
                        )
                    )
                }
            }
            i++
        }

        return composables
    }

    private fun determineRole(
        isInput: Boolean,
        isLeaf: Boolean,
        isContainer: Boolean,
        funcText: String
    ): ComponentRole {
        if (isInput) return ComponentRole.INPUT_SURFACE

        if (isLeaf) {
            val hasContainerStructures = funcText.contains("Column(") || funcText.contains("Row(") || funcText.contains("Card(") || funcText.contains("LazyColumn(")
            if (!hasContainerStructures) {
                return ComponentRole.LEAF_OR_SMALL
            }
            return ComponentRole.SCREEN_OR_CONTAINER
        }

        if (isContainer) {
            return ComponentRole.SCREEN_OR_CONTAINER
        }

        return ComponentRole.UNKNOWN
    }

    private fun extractModifierChains(funcText: String): List<ParsedModifierChain> {
        val chains = mutableListOf<ParsedModifierChain>()

        val chainRegex = Regex("(?:[a-zA-Z0-9_]*\\.)?Modifier((?:\\.[a-zA-Z0-9_]+\\([^)]*\\))+)")
        val matches = chainRegex.findAll(funcText)

        for (match in matches) {
            val fullChainText = match.value
            val callsText = match.groupValues[1]

            val calls = extractModifierCalls(callsText)
            val hasScroll = calls.any {
                it.contains("verticalScroll") || it.contains("horizontalScroll") || it.contains("scrollable")
            }

            val targetPrimitive = findTargetPrimitiveForChain(funcText, match.range.first)

            chains.add(
                ParsedModifierChain(
                    rawText = fullChainText,
                    calls = calls,
                    hasScrollModifier = hasScroll,
                    targetPrimitive = targetPrimitive
                )
            )
        }

        if (chains.isEmpty()) {
            val calls = extractModifierCalls(funcText)
            if (calls.isNotEmpty()) {
                val hasScroll = calls.any {
                    it.contains("verticalScroll") || it.contains("horizontalScroll") || it.contains("scrollable")
                }
                chains.add(
                    ParsedModifierChain(
                        rawText = funcText,
                        calls = calls,
                        hasScrollModifier = hasScroll,
                        targetPrimitive = null
                    )
                )
            }
        }

        return chains
    }

    private fun findTargetPrimitiveForChain(funcText: String, chainIndex: Int): String? {
        val textBefore = funcText.substring(0, chainIndex).trimEnd()
        val primitives = listOf("LazyColumn", "LazyRow", "LazyVerticalGrid", "LazyHorizontalGrid", "Column", "Row", "Box", "Card", "Scaffold")
        for (prim in primitives) {
            if (Regex("$prim\\s*\\(\\s*(?:modifier\\s*=\\s*)?$").containsMatchIn(textBefore)) {
                return prim
            }
        }
        return null
    }

    private fun extractModifierCalls(text: String): List<String> {
        val calls = mutableListOf<String>()
        val length = text.length
        var i = 0
        while (i < length) {
            if (text[i] == '.') {
                var j = i + 1
                while (j < length && (text[j].isLetterOrDigit() || text[j] == '_')) {
                    j++
                }
                if (j < length && text[j] == '(') {
                    var parenCount = 1
                    var k = j + 1
                    while (k < length && parenCount > 0) {
                        if (text[k] == '(') parenCount++
                        else if (text[k] == ')') parenCount--
                        k++
                    }
                    val fullCall = text.substring(i + 1, k).replace(Regex("\\s+"), " ")
                    calls.add(fullCall)
                    i = k - 1
                }
            }
            i++
        }
        return calls
    }

    private fun hasAdaptiveCues(funcName: String, funcText: String): Boolean {
        val lowerText = funcText.lowercase()
        val lowerName = funcName.lowercase()

        val textCues = lowerText.contains("windowsizeclass") ||
            lowerText.contains("boxwithconstraints") ||
            lowerText.contains("calculatewindowsizeclass") ||
            lowerText.contains("config.screenwidthdp") ||
            lowerText.contains("localconfiguration.current") ||
            lowerText.contains("displayfeature") ||
            lowerText.contains("foldingfeature") ||
            lowerText.contains("flowrow") ||
            lowerText.contains("flowcolumn") ||
            lowerText.contains("navigationsuitescaffold") ||
            lowerText.contains("navigationrail") ||
            lowerText.contains("gridcells.adaptive") ||
            lowerText.contains("gridcells.fixed") ||
            lowerText.contains("listdetailpanescaffold") ||
            lowerText.contains("supportingpanescaffold") ||
            lowerText.contains("threepanescaffold") ||
            lowerText.contains("rememberadaptiveinfo") ||
            lowerText.contains("remembercolumnwidth") ||
            lowerText.contains("rememberresponsivevalue") ||
            lowerText.contains("isexpanded") ||
            lowerText.contains("panestate")

        val nameCues = lowerName.contains("adaptive") || lowerName.contains("responsive")

        return textCues || nameCues
    }

    private fun isInputSurfaceComponent(funcName: String, funcText: String): Boolean {
        val nameLower = funcName.lowercase()
        val textLower = funcText.lowercase()

        val nameMatches = nameLower.contains("otp") ||
            nameLower.contains("input") ||
            nameLower.contains("textfield") ||
            nameLower.contains("pin") ||
            nameLower.contains("cell") ||
            nameLower.contains("selector")

        val textMatches = textLower.contains("basictextfield") ||
            textLower.contains("textfield(") ||
            textLower.contains("outlinedtextfield(") ||
            textLower.contains("checkbox(") ||
            textLower.contains("switch(") ||
            textLower.contains("radiobutton(")

        return nameMatches || textMatches
    }

    private fun isContainerComponent(funcName: String, funcText: String): Boolean {
        val nameLower = funcName.lowercase()
        val textLower = funcText.lowercase()

        val nameMatches = screenOrContainerKeywords.any { nameLower.contains(it) }
        val textMatches = textLower.contains("scaffold") ||
            textLower.contains("navhost") ||
            textLower.contains("listdetailpanescaffold") ||
            textLower.contains("navigationsuitescaffold") ||
            textLower.contains("supportingpanescaffold")

        return nameMatches || textMatches
    }

    private fun isLeafComponent(funcName: String, funcText: String, isInput: Boolean): Boolean {
        if (isInput) return true

        val nameLower = funcName.lowercase()
        val textLower = funcText.lowercase()

        val nameMatches = leafOrSmallKeywords.any { nameLower.contains(it) }
        val pureLeafGraphics = (textLower.contains("image(") || textLower.contains("icon(") || textLower.contains("spacer(") || textLower.contains("divider(")) &&
            !textLower.contains("text(") && !textLower.contains("column(") && !textLower.contains("card(")

        return nameMatches || pureLeafGraphics
    }

    private fun hasInternalScrollState(funcText: String): Boolean {
        val lower = funcText.lowercase()
        return lower.contains("verticalscroll(") ||
            lower.contains("horizontalscroll(") ||
            lower.contains("rememberscrollstate()") ||
            lower.contains("lazycolumn") ||
            lower.contains("lazyrow") ||
            lower.contains("lazyverticalgrid")
    }

    private fun hasVariableTextContent(funcText: String): Boolean {
        val lower = funcText.lowercase()
        return lower.contains("text(") || lower.contains("basictext(")
    }

    private fun hasScrollableContainer(funcText: String): Boolean {
        val lower = funcText.lowercase()
        return lower.contains("lazycolumn") ||
            lower.contains("lazyrow") ||
            lower.contains("lazyverticalgrid") ||
            lower.contains("lazyhorizontalgrid") ||
            lower.contains("verticalscroll") ||
            lower.contains("horizontalscroll") ||
            lower.contains("items(") ||
            lower.contains("itemsindexed(")
    }
}
