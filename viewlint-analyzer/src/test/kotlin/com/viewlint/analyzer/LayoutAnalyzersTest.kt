package com.viewlint.analyzer

import com.viewlint.core.ViewlintContext
import com.viewlint.core.LayoutResource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class LayoutAnalyzersTest {

    @Test
    fun `FixedRootWidthAnalyzer detects fixed root width when no alternative variant exists`() {
        val dir = Files.createTempDirectory("viewlint-layout-test").toFile()
        val layoutFile = File(dir, "res/layout/activity_main.xml")
        layoutFile.parentFile.mkdirs()
        layoutFile.writeText(
            """
            <LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
                android:layout_width="400dp"
                android:layout_height="match_parent">
            </LinearLayout>
            """.trimIndent()
        )

        try {
            val context = ViewlintContext(
                projectRoot = dir.toPath(),
                manifests = emptyList(),
                layouts = listOf(LayoutResource("activity_main", "layout", layoutFile.toPath()))
            )

            val findings = FixedRootWidthAnalyzer().analyze(context)
            assertEquals(1, findings.size)
            assertEquals("FF003", findings.first().ruleId)
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `FixedRootWidthAnalyzer ignores fixed root width when adaptive variant exists`() {
        val dir = Files.createTempDirectory("viewlint-layout-test").toFile()
        val baseLayout = File(dir, "res/layout/activity_main.xml")
        baseLayout.parentFile.mkdirs()
        baseLayout.writeText(
            """
            <LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
                android:layout_width="400dp"
                android:layout_height="match_parent">
            </LinearLayout>
            """.trimIndent()
        )

        val swLayout = File(dir, "res/layout-sw600dp/activity_main.xml")
        swLayout.parentFile.mkdirs()
        swLayout.writeText(
            """
            <LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
                android:layout_width="match_parent"
                android:layout_height="match_parent">
            </LinearLayout>
            """.trimIndent()
        )

        try {
            val context = ViewlintContext(
                projectRoot = dir.toPath(),
                manifests = emptyList(),
                layouts = listOf(
                    LayoutResource("activity_main", "layout", baseLayout.toPath()),
                    LayoutResource("activity_main", "layout-sw600dp", swLayout.toPath())
                )
            )

            val findings = FixedRootWidthAnalyzer().analyze(context)
            assertTrue(findings.isEmpty())
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `FixedDimensionAnalyzer detects fixed large inner dimensions`() {
        val dir = Files.createTempDirectory("viewlint-layout-test").toFile()
        val layoutFile = File(dir, "res/layout/activity_main.xml")
        layoutFile.parentFile.mkdirs()
        layoutFile.writeText(
            """
            <LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
                android:layout_width="match_parent"
                android:layout_height="match_parent">
                <View
                    android:layout_width="400dp"
                    android:layout_height="500dp" />
            </LinearLayout>
            """.trimIndent()
        )

        try {
            val context = ViewlintContext(
                projectRoot = dir.toPath(),
                manifests = emptyList(),
                layouts = listOf(LayoutResource("activity_main", "layout", layoutFile.toPath()))
            )

            val findings = FixedDimensionAnalyzer().analyze(context)
            assertEquals(1, findings.size)
            assertEquals("FF004", findings.first().ruleId)
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `LayoutPositioningAnalyzer detects AbsoluteLayout`() {
        val dir = Files.createTempDirectory("viewlint-layout-test").toFile()
        val layoutFile = File(dir, "res/layout/activity_main.xml")
        layoutFile.parentFile.mkdirs()
        layoutFile.writeText(
            """
            <AbsoluteLayout xmlns:android="http://schemas.android.com/apk/res/android"
                android:layout_width="match_parent"
                android:layout_height="match_parent">
            </AbsoluteLayout>
            """.trimIndent()
        )

        try {
            val context = ViewlintContext(
                projectRoot = dir.toPath(),
                manifests = emptyList(),
                layouts = listOf(LayoutResource("activity_main", "layout", layoutFile.toPath()))
            )

            val findings = LayoutPositioningAnalyzer().analyze(context)
            assertEquals(1, findings.size)
            assertEquals("FF006", findings.first().ruleId)
        } finally {
            dir.deleteRecursively()
        }
    }
}
