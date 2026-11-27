package com.viewlint.analyzer

import com.viewlint.core.Confidence
import com.viewlint.core.ViewlintContext
import com.viewlint.core.ViewlintFinding
import com.viewlint.core.ViewlintRule
import com.viewlint.core.Severity
import java.io.File
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory

class OrientationRestrictionAnalyzer : ViewlintRule {

    override val id = "FF001"

    override val description =
        "Detects explicit activity orientation restrictions in AndroidManifest.xml"

    override fun analyze(
        context: ViewlintContext
    ): List<ViewlintFinding> {
        return context.manifests.flatMap { path ->
            analyze(path.toFile())
        }
    }

    private val restrictedOrientations = setOf(
        "portrait",
        "landscape",
        "reversePortrait",
        "reverseLandscape",
        "sensorPortrait",
        "sensorLandscape",
        "locked"
    )

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

            buildList {
                for (index in 0 until activities.length) {
                    val activity = activities.item(index)
                    val orientation = activity.attributes
                        ?.getNamedItemNS(
                            "http://schemas.android.com/apk/res/android",
                            "screenOrientation"
                        )
                        ?.nodeValue

                    if (orientation in restrictedOrientations) {
                        val name = activity.attributes
                            ?.getNamedItemNS(
                                "http://schemas.android.com/apk/res/android",
                                "name"
                            )
                            ?.nodeValue ?: "Unknown activity"

                        add(
                            ViewlintFinding(
                                ruleId = id,
                                title = "Activity orientation is restricted",
                                message = "$name explicitly restricts orientation to '$orientation'.",
                                severity = Severity.WARNING,
                                confidence = Confidence.MEDIUM,
                                file = manifest.path,
                                evidence = "android:screenOrientation=\"$orientation\"",
                                remediation = "Review whether this restriction is necessary. " +
                                    "Explicit orientation restrictions can degrade user experience on foldables, tablets, " +
                                    "and multi-window environments. Note that for apps targeting Android 16 (API 36), " +
                                    "orientation restrictions are ignored on form factors with smallest width >= 600dp."
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
}
