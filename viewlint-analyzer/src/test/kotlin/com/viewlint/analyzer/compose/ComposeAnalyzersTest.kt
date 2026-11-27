package com.viewlint.analyzer.compose

import com.viewlint.analyzer.compose.rules.HardcodedDimensionAnalyzer
import com.viewlint.analyzer.compose.rules.MissingAdaptiveHandlingAnalyzer
import com.viewlint.analyzer.compose.rules.RequiredSizeAnalyzer
import com.viewlint.analyzer.compose.rules.RestrictiveModifierChainsAnalyzer
import com.viewlint.core.ViewlintContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class ComposeAnalyzersTest {

    @Test
    fun `detects hardcoded dimensions in composable UI`() {
        val dir = Files.createTempDirectory("viewlint-compose-test").toFile()
        val ktFile = File(dir, "src/main/java/com/example/MyComposable.kt")
        ktFile.parentFile.mkdirs()
        ktFile.writeText(
            """
            package com.example
            import androidx.compose.runtime.Composable
            import androidx.compose.ui.Modifier
            import androidx.compose.ui.unit.dp

            @Composable
            fun FixedSizeComponent() {
                Box(modifier = Modifier.width(400.dp))
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
            assertEquals(1, findings.size)
            assertEquals("FF007", findings.first().ruleId)
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `ignores hardcoded dimensions when adaptive cues are present`() {
        val dir = Files.createTempDirectory("viewlint-compose-test").toFile()
        val ktFile = File(dir, "src/main/java/com/example/AdaptiveComponent.kt")
        ktFile.parentFile.mkdirs()
        ktFile.writeText(
            """
            package com.example
            import androidx.compose.runtime.Composable
            import androidx.compose.ui.Modifier
            import androidx.compose.ui.unit.dp

            @Composable
            fun AdaptiveComponent() {
                val windowSize = calculateWindowSizeClass()
                Box(modifier = Modifier.width(400.dp))
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
            assertTrue(findings.isEmpty())
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `detects required size modifier`() {
        val dir = Files.createTempDirectory("viewlint-compose-test").toFile()
        val ktFile = File(dir, "src/main/java/com/example/RequiredComponent.kt")
        ktFile.parentFile.mkdirs()
        ktFile.writeText(
            """
            package com.example
            import androidx.compose.runtime.Composable
            import androidx.compose.ui.Modifier
            import androidx.compose.ui.unit.dp

            @Composable
            fun RequiredComponent() {
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
            assertEquals(1, findings.size)
            assertEquals("FF008", findings.first().ruleId)
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `detects restrictive modifier chains`() {
        val dir = Files.createTempDirectory("viewlint-compose-test").toFile()
        val ktFile = File(dir, "src/main/java/com/example/ChainedComponent.kt")
        ktFile.parentFile.mkdirs()
        ktFile.writeText(
            """
            package com.example
            import androidx.compose.runtime.Composable
            import androidx.compose.ui.Modifier
            import androidx.compose.ui.unit.dp

            @Composable
            fun ChainedComponent() {
                Box(modifier = Modifier.width(300.dp).height(400.dp))
            }
            """.trimIndent()
        )

        try {
            val context = ViewlintContext(
                projectRoot = dir.toPath(),
                manifests = emptyList(),
                kotlinSources = listOf(ktFile.toPath())
            )

            val findings = RestrictiveModifierChainsAnalyzer().analyze(context)
            assertEquals(1, findings.size)
            assertEquals("FF010", findings.first().ruleId)
        } finally {
            dir.deleteRecursively()
        }
    }
}
