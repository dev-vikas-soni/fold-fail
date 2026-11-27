package io.github.devvikassoni.foldfail.analyzer

import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.File
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory

data class LayoutNode(
    val tagName: String,
    val width: String?,
    val height: String?,
    val weight: String?,
    val isScrollContainer: Boolean,
    val attributes: Map<String, String>,
    val children: List<LayoutNode>
)

data class ParsedLayout(
    val file: File,
    val root: LayoutNode?,
    val error: String? = null
)

object LayoutParser {

    private val androidNamespace = "http://schemas.android.com/apk/res/android"

    fun parse(file: File): ParsedLayout {
        if (!file.exists() || !file.isFile) {
            return ParsedLayout(file, null, "File does not exist or is not a file")
        }

        return try {
            val factory = DocumentBuilderFactory.newInstance().apply {
                isNamespaceAware = true
                setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true)
                try {
                    setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
                } catch (_: Exception) {
                }
                try {
                    setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "")
                } catch (_: Exception) {
                }
                try {
                    setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "")
                } catch (_: Exception) {
                }
            }

            val document = factory.newDocumentBuilder().parse(file)
            val rootElement = document.documentElement ?: return ParsedLayout(file, null, "Empty document element")

            val rootNode = parseElement(rootElement)
            ParsedLayout(file, rootNode)
        } catch (e: Exception) {
            ParsedLayout(file, null, e.message ?: "Parsing exception")
        }
    }

    private fun parseElement(element: Element): LayoutNode {
        val tagName = element.tagName ?: "Unknown"
        val attrs = mutableMapOf<String, String>()

        val attributeMap = element.attributes
        if (attributeMap != null) {
            for (i in 0 until attributeMap.length) {
                val attr = attributeMap.item(i)
                val name = attr.localName ?: attr.nodeName ?: continue
                val value = attr.nodeValue ?: continue
                attrs[name] = value
            }
        }

        val width = attrs["layout_width"]
        val height = attrs["layout_height"]
        val weight = attrs["layout_weight"]

        val scrollKeywords = listOf("scroll", "recycler", "list", "pager", "grid")
        val isScrollContainer = scrollKeywords.any { tagName.contains(it, ignoreCase = true) }

        val children = mutableListOf<LayoutNode>()
        val childNodes = element.childNodes
        if (childNodes != null) {
            for (i in 0 until childNodes.length) {
                val child = childNodes.item(i)
                if (child.nodeType == Node.ELEMENT_NODE) {
                    children.add(parseElement(child as Element))
                }
            }
        }

        return LayoutNode(
            tagName = tagName,
            width = width,
            height = height,
            weight = weight,
            isScrollContainer = isScrollContainer,
            attributes = attrs,
            children = children
        )
    }
}
