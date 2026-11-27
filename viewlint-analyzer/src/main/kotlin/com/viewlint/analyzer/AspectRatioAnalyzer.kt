package com.viewlint.analyzer

import com.viewlint.core.Confidence
import com.viewlint.core.Severity
import com.viewlint.core.ViewlintContext
import com.viewlint.core.ViewlintFinding
import com.viewlint.core.ViewlintRule
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
            val usesSdkNodes = document.getElementsByTagName("uses-sdk")
            val targetSdkVersion = if (usesSdkNodes.length > 0) {
                extractAttribute(usesSdkNodes.item(0), "targetSdkVersion")?.let { parseTargetSdkVersion(it) }
            } else null

            // Android Platform Rule for omitted resizeableActivity:
            // - Apps targeting API 24 (Android 7.0 Nougat) or higher default to true.
            // - Apps targeting below API 24 default to false.
            // - When targetSdkVersion is unresolved/missing, conservatively default to false to ensure potential restrictions are audited.
            val platformDefaultResizeable = when {
                targetSdkVersion != null -> targetSdkVersion >= 24
                else -> false // Conservative fallback when targetSdkVersion is omitted
            }

            val applications = document.getElementsByTagName("application")
            val appNode = if (applications.length > 0) applications.item(0) else null
            val globalMaxAspectRatio = extractAttribute(appNode, "maxAspectRatio")
            val globalMinAspectRatio = extractAttribute(appNode, "minAspectRatio")
            val appResizeAttr = extractAttribute(appNode, "resizeableActivity")
            val globalResizeable = appResizeAttr?.toBoolean() ?: platformDefaultResizeable

            val activities = document.getElementsByTagName("activity")

            buildList {
                for (index in 0 until activities.length) {
                    val activity = activities.item(index)
                    val activityResizeAttr = extractAttribute(activity, "resizeableActivity")
                    val effectiveResizable = activityResizeAttr?.toBoolean() ?: globalResizeable

                    // If effectiveResizable is true, aspect-ratio attributes are ignored by the Android platform
                    if (effectiveResizable) continue

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
                                remediation = "Remove maxAspectRatio restrictions to support ultra-wide foldables, split-screen, and multi-window displays. Note that on Android 16 (API 36), aspect-ratio restrictions are ignored on displays with smallest width >= 600dp unless opted out, but restrictions remain active on compact screens."
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
                                remediation = "Remove minAspectRatio restrictions to ensure compatibility across varied screen proportions. Note that on Android 16 (API 36), aspect-ratio restrictions are ignored on displays with smallest width >= 600dp unless opted out, but restrictions remain active on compact screens."
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

    private fun parseTargetSdkVersion(targetSdkStr: String): Int? {
        val trimmed = targetSdkStr.trim()
        return trimmed.toIntOrNull() ?: when (trimmed.uppercase()) {
            "BAKLAVA" -> 36
            "VANILLA_ICE_CREAM" -> 35
            "UPSIDE_DOWN_CAKE" -> 34
            "TIRAMISU" -> 33
            "S", "SV2" -> 31
            "R" -> 30
            "Q" -> 29
            "P" -> 28
            "O", "OMR1" -> 26
            "N", "NMR1" -> 24
            else -> null
        }
    }

    private fun extractAttribute(node: org.w3c.dom.Node?, attrName: String): String? {
        if (node == null) return null
        return node.attributes
            ?.getNamedItemNS("http://schemas.android.com/apk/res/android", attrName)
            ?.nodeValue
    }
}
