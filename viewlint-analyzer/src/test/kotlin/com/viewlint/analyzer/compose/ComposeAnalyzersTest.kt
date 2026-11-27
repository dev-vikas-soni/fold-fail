package com.viewlint.analyzer.compose

import com.viewlint.analyzer.compose.rules.HardcodedDimensionAnalyzer
import com.viewlint.analyzer.compose.rules.MissingAdaptiveHandlingAnalyzer
import com.viewlint.analyzer.compose.rules.RequiredSizeAnalyzer
import com.viewlint.analyzer.compose.rules.RestrictiveModifierChainsAnalyzer
import com.viewlint.core.Confidence
import com.viewlint.core.ViewlintContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class ComposeAnalyzersTest {

    @Test
    fun `tests threshold boundaries 48dp 64dp 65dp 120dp 121dp in FF007`() {
        val dir = Files.createTempDirectory("viewlint-threshold-test").toFile()
        val ktFile = File(dir, "src/main/java/com/example/ThresholdTest.kt")
        ktFile.parentFile.mkdirs()
        ktFile.writeText(
            """
            package com.example
            import androidx.compose.runtime.Composable
            import androidx.compose.ui.Modifier
            import androidx.compose.ui.unit.dp

            @Composable
            fun TouchTarget48() {
                Box(modifier = Modifier.size(48.dp))
            }

            @Composable
            fun Avatar64() {
                Box(modifier = Modifier.size(64.dp))
            }

            @Composable
            fun Component65() {
                Box(modifier = Modifier.height(65.dp))
            }

            @Composable
            fun Component120() {
                Box(modifier = Modifier.height(120.dp))
            }

            @Composable
            fun BannerAdminCard121() {
                Box(modifier = Modifier.height(121.dp))
            }

            @Composable
            fun MinBoundFlexible() {
                Box(modifier = Modifier.widthIn(min = 48.dp))
            }
            """.trimIndent()
        )

        try {
            val context = ViewlintContext(
                projectRoot = dir.toPath(),
                manifests = emptyList(),
                kotlinSources = listOf(ktFile.toPath())
            )

            val findings = HardcodedDimensionAnalyzer().analyze(context)

            // TouchTarget48 and MinBoundFlexible should be excluded
            assertTrue(findings.none { it.message.contains("TouchTarget48") })
            assertTrue(findings.none { it.message.contains("MinBoundFlexible") })

            // Component65 should be reported with Confidence.MEDIUM
            val finding65 = findings.firstOrNull { it.message.contains("Component65") }
            assertTrue(finding65 != null)
            assertEquals(Confidence.MEDIUM, finding65!!.confidence)

            // Component120 should be reported with Confidence.MEDIUM
            val finding120 = findings.firstOrNull { it.message.contains("Component120") }
            assertTrue(finding120 != null)
            assertEquals(Confidence.MEDIUM, finding120!!.confidence)

            // BannerAdminCard121 should be reported with Confidence.HIGH
            val finding121 = findings.firstOrNull { it.message.contains("BannerAdminCard121") }
            assertTrue(finding121 != null)
            assertEquals(Confidence.HIGH, finding121!!.confidence)
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `ignores standard small component dimensions like icons avatars spacers and chips`() {
        val dir = Files.createTempDirectory("viewlint-compose-small-test").toFile()
        val ktFile = File(dir, "src/main/java/com/example/SmallComponents.kt")
        ktFile.parentFile.mkdirs()
        ktFile.writeText(
            """
            package com.example
            import androidx.compose.runtime.Composable
            import androidx.compose.ui.Modifier
            import androidx.compose.ui.unit.dp

            @Composable
            fun UserAvatarIcon() {
                Icon(modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Chip(modifier = Modifier.height(48.dp))
            }
            """.trimIndent()
        )

        try {
            val context = ViewlintContext(
                projectRoot = dir.toPath(),
                manifests = emptyList(),
                kotlinSources = listOf(ktFile.toPath())
            )

            val findings = HardcodedDimensionAnalyzer().analyze(context)
            assertTrue("Expected no FF007 findings for standard small icon, avatar, spacer, or chip dimensions", findings.isEmpty())
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `FF008 requiredSize assigns confidence based on size`() {
        val dir = Files.createTempDirectory("viewlint-required-test").toFile()
        val ktFile = File(dir, "src/main/java/com/example/RequiredComponent.kt")
        ktFile.parentFile.mkdirs()
        ktFile.writeText(
            """
            package com.example
            import androidx.compose.runtime.Composable
            import androidx.compose.ui.Modifier
            import androidx.compose.ui.unit.dp

            @Composable
            fun SmallIconRequired() {
                Box(modifier = Modifier.requiredSize(24.dp))
            }

            @Composable
            fun LargeCardRequired() {
                Box(modifier = Modifier.requiredWidth(300.dp))
            }
            """.trimIndent()
        )

        try {
            val context = ViewlintContext(
                projectRoot = dir.toPath(),
                manifests = emptyList(),
                kotlinSources = listOf(ktFile.toPath())
            )

            val findings = RequiredSizeAnalyzer().analyze(context)
            assertEquals(2, findings.size)

            val smallFinding = findings.first { it.message.contains("SmallIconRequired") }
            assertEquals(Confidence.LOW, smallFinding.confidence)

            val largeFinding = findings.first { it.message.contains("LargeCardRequired") }
            assertEquals(Confidence.HIGH, largeFinding.confidence)
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `FF009 ignores non-composable utility functions and leaf controls`() {
        val dir = Files.createTempDirectory("viewlint-ff009-test").toFile()
        val ktFile = File(dir, "src/main/java/com/example/UtilsAndLeafs.kt")
        ktFile.parentFile.mkdirs()
        ktFile.writeText(
            """
            package com.example
            import androidx.compose.runtime.Composable
            import androidx.compose.ui.Modifier
            import androidx.compose.ui.unit.dp

            // Utility function (not a composable)
            fun calculatePrice(base: Double): Double {
                val factor = 1.2
                return base * factor
            }

            @Composable
            fun QuantitySelectorChip() {
                Box(modifier = Modifier.width(100.dp).height(40.dp))
            }

            @Composable
            fun DashboardMainScreen() {
                Column(modifier = Modifier.width(400.dp).height(800.dp)) {
                    Text("Dashboard Content")
                }
            }
            """.trimIndent()
        )

        try {
            val context = ViewlintContext(
                projectRoot = dir.toPath(),
                manifests = emptyList(),
                kotlinSources = listOf(ktFile.toPath())
            )

            val findings = MissingAdaptiveHandlingAnalyzer().analyze(context)
            // Should only flag DashboardMainScreen (screen container), ignoring calculatePrice and QuantitySelectorChip
            assertEquals(1, findings.size)
            assertEquals("FF009", findings.first().ruleId)
            assertTrue(findings.first().message.contains("DashboardMainScreen"))
        } finally {
            dir.deleteRecursively()
        }
    }
}
