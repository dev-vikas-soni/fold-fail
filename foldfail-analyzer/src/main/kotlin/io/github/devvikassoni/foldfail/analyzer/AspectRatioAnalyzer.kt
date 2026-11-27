package io.github.devvikassoni.foldfail.analyzer

import io.github.devvikassoni.foldfail.core.Confidence
import io.github.devvikassoni.foldfail.core.FoldFailContext
import io.github.devvikassoni.foldfail.core.FoldFailFinding
import io.github.devvikassoni.foldfail.core.FoldFailRule
import io.github.devvikassoni.foldfail.core.Severity
import java.io.File
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory

class AspectRatioAnalyzer : FoldFailRule {

    override val id = "FF002"

    override val description =
        "Detects explicit activity aspect ratio restrictions in AndroidManifest.xml"

    override fun analyze(
        context: FoldFailContext
    ): List<FoldFailFinding> {
        return context.manifests.flatMap { path ->
            analyze(path.toFile())
        }
    }

    fun analyze(manifest: File): List<FoldFailFinding> {
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
            val applications = document.getElementsByTagName("application")

            // App-level resizableActivity default
            var appResizable: Boolean? = null
            if (applications.length > 0) {
                val appNode = applications.item(0)
                val appResizeAttr = appNode.attributes
                    ?.getNamedItemNS(
                        "http://schemas.android.com/apk/res/android",
                        "resizeableActivity"
                    )
                    ?.nodeValue
                if (appResizeAttr != null) {
                    appResizable = appResizeAttr.toBoolean()
                }
            }

            val activities = document.getElementsByTagName("activity")

            buildList {
                for (index in 0 until activities.length) {
                    val activity = activities.item(index)
                    val maxAspect = activity.attributes
                        ?.getNamedItemNS(
                            "http://schemas.android.com/apk/res/android",
                            "maxAspectRatio"
                        )
                        ?.nodeValue

                    val minAspect = activity.attributes
                        ?.getNamedItemNS(
                            "http://schemas.android.com/apk/res/android",
                            "minAspectRatio"
                        )
                        ?.nodeValue

                    if (maxAspect != null || minAspect != null) {
                        // Check activity-level override for resizeableActivity
                        val activityResizeAttr = activity.attributes
                            ?.getNamedItemNS(
                                "http://schemas.android.com/apk/res/android",
                                "resizeableActivity"
                            )
                            ?.nodeValue

                        val effectiveResizable = if (activityResizeAttr != null) {
                            activityResizeAttr.toBoolean()
                        } else {
                            appResizable ?: false // Absent resizeableActivity with aspect ratio defaults to non-resizable on constrained form factors
                        }

                        // If effectiveResizable is true, aspect-ratio attributes are ignored by the platform
                        if (effectiveResizable) {
                            continue
                        }

                        val name = activity.attributes
                            ?.getNamedItemNS(
                                "http://schemas.android.com/apk/res/android",
                                "name"
                            )
                            ?.nodeValue ?: "Unknown activity"

                        val evidenceParts = mutableListOf<String>()
                        if (maxAspect != null) evidenceParts.add("android:maxAspectRatio=\"$maxAspect\"")
                        if (minAspect != null) evidenceParts.add("android:minAspectRatio=\"$minAspect\"")
                        if (activityResizeAttr != null) evidenceParts.add("android:resizeableActivity=\"$activityResizeAttr\"")

                        add(
                            FoldFailFinding(
                                ruleId = id,
                                title = "Activity aspect ratio is restricted",
                                message = "$name restricts aspect ratio (maxAspectRatio='$maxAspect', minAspectRatio='$minAspect').",
                                severity = Severity.WARNING,
                                confidence = Confidence.MEDIUM,
                                file = manifest.path,
                                evidence = evidenceParts.joinToString(" "),
                                remediation = "Review whether fixed aspect ratio restrictions are necessary. " +
                                    "Fixed aspect ratios can cause letterboxing or pillarboxing on foldables, tablets, " +
                                    "and desktop windowing environments. Note that for applications targeting Android 16 (API 36), " +
                                    "aspect-ratio restrictions are ignored on form factors with smallest width >= 600dp if " +
                                    "resizeableActivity is not explicitly false."
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            listOf(
                FoldFailFinding(
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
