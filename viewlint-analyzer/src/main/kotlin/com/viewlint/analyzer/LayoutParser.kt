package com.viewlint.analyzer

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

            val doc = factory.newDocumentBuilder().parse(file)
            val rootElement = doc.documentElement ?: return ParsedLayout(file, null, "Empty document element")
            val rootNode = parseNode(rootElement)
            ParsedLayout(file, rootNode)
        } catch (e: Exception) {
            ParsedLayout(file, null, e.message ?: "XML parsing exception")
        }
    }

    private fun parseNode(element: Element): LayoutNode {
        val tagName = element.tagName
        val width = element.getAttributeNS(androidNamespace, "layout_width")
            .ifEmpty { element.getAttribute("android:layout_width") }
        val height = element.getAttributeNS(androidNamespace, "layout_height")
            .ifEmpty { element.getAttribute("android:layout_height") }
        val weight = element.getAttributeNS(androidNamespace, "layout_weight")
            .ifEmpty { element.getAttribute("android:layout_weight") }

        val isScrollContainer = tagName.contains("ScrollView", ignoreCase = true) ||
            tagName.contains("HorizontalScrollView", ignoreCase = true) ||
            tagName.contains("NestedScrollView", ignoreCase = true) ||
            tagName.contains("RecyclerView", ignoreCase = true) ||
            tagName.contains("ViewPager", ignoreCase = true)

        val attrs = mutableMapOf<String, String>()
        val attrMap = element.attributes
        if (attrMap != null) {
            for (i in 0 until attrMap.length) {
                val attr = attrMap.item(i)
                attrs[attr.nodeName] = attr.nodeValue
            }
        }

        val children = mutableListOf<LayoutNode>()
        val childNodes = element.childNodes
        if (childNodes != null) {
            for (i in 0 until childNodes.length) {
                val child = childNodes.item(i)
                if (child.nodeType == Node.ELEMENT_NODE) {
                    children.add(parseNode(child as Element))
                }
            }
        }

        return LayoutNode(
            tagName = tagName,
            width = width.ifEmpty { null },
            height = height.ifEmpty { null },
            weight = weight.ifEmpty { null },
            isScrollContainer = isScrollContainer,
            attributes = attrs,
            children = children
        )
    }
}
