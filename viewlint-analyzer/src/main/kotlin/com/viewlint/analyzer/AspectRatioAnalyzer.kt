package com.viewlint.analyzer

import com.viewlint.core.Confidence
import com.viewlint.core.ViewlintContext
import com.viewlint.core.ViewlintFinding
import com.viewlint.core.ViewlintRule
import com.viewlint.core.Severity
import java.io.File
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory

class AspectRatioAnalyzer : ViewlintRule {

    override val id = "FF002"

    override val description =
        "Detects explicit activity aspect ratio restrictions in AndroidManifest.xml"

    override fun analyze(
        context: ViewlintContext
    ): List<ViewlintFinding> {
        return context.manifests.flatMap { path ->
            analyze(path.toFile())
        }
    }

    fun analyze(manifest: File): List<ViewlintFinding> {
        if (!manifest.exists() || !manifest.isFile) return emptyList()

        return try {
            val factory = DocumentBuilderFactory.newInstance().apply {
                isNamespaceAware = true
                setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true)
                try {
                    setFeature(
                        "http://apache.org/xml/features/disallow-doctype-decl",
                        true
                    )
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

            val document = factory.newDocumentBuilder().parse(manifest)
            val activities = document.getElementsByTagName("activity")
            val applications = document.getElementsByTagName("application")

            val globalMaxAspectRatio = extractAttribute(applications.item(0), "maxAspectRatio")
            val globalMinAspectRatio = extractAttribute(applications.item(0), "minAspectRatio")
            val globalResizeable = extractAttribute(applications.item(0), "resizeableActivity")?.toBoolean() ?: false

            buildList {
                for (index in 0 until activities.length) {
                    val activity = activities.item(index)
                    val resizeable = extractAttribute(activity, "resizeableActivity")?.toBoolean() ?: globalResizeable
                    if (resizeable) continue

                    val maxAspect = extractAttribute(activity, "maxAspectRatio") ?: globalMaxAspectRatio
                    val minAspect = extractAttribute(activity, "minAspectRatio") ?: globalMinAspectRatio

                    val name = extractAttribute(activity, "name") ?: "Unknown activity"

                    if (maxAspect != null) {
                        add(
                            ViewlintFinding(
                                ruleId = id,
                                title = "Activity maximum aspect ratio is restricted",
                                message = "$name restricts maxAspectRatio to $maxAspect.",
                                severity = Severity.WARNING,
                                confidence = Confidence.MEDIUM,
                                file = manifest.path,
                                evidence = "android:maxAspectRatio=\"$maxAspect\"",
                                remediation = "Remove maxAspectRatio restrictions to support ultra-wide foldables, split-screen, and multi-window displays."
                            )
                        )
                    }

                    if (minAspect != null) {
                        add(
                            ViewlintFinding(
                                ruleId = id,
                                title = "Activity minimum aspect ratio is restricted",
                                message = "$name restricts minAspectRatio to $minAspect.",
                                severity = Severity.WARNING,
                                confidence = Confidence.MEDIUM,
                                file = manifest.path,
                                evidence = "android:minAspectRatio=\"$minAspect\"",
                                remediation = "Remove minAspectRatio restrictions to ensure compatibility across varied screen proportions."
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            listOf(
                ViewlintFinding(
                    ruleId = id,
                    title = "Failed to parse AndroidManifest.xml",
                    message = "Could not parse manifest at ${manifest.path}: ${e.message}",
                    severity = Severity.ERROR,
                    confidence = Confidence.HIGH,
                    file = manifest.path,
                    evidence = e.message ?: "XML parsing exception",
                    remediation = "Ensure AndroidManifest.xml is well-formed XML."
                )
            )
        }
    }

    private fun extractAttribute(node: org.w3c.dom.Node?, attrName: String): String? {
        if (node == null) return null
        return node.attributes
            ?.getNamedItemNS("http://schemas.android.com/apk/res/android", attrName)
            ?.nodeValue
    }
}
