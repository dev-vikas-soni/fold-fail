package com.viewlint.gradle

import com.viewlint.analyzer.ViewlintRuleRegistry
import com.viewlint.core.ViewlintContext
import com.viewlint.core.ViewlintEngine
import com.viewlint.core.LayoutResource
import com.viewlint.core.Severity
import com.viewlint.core.reporting.ViewlintReport
import com.viewlint.core.reporting.ViewlintReportMetadata
import com.viewlint.core.reporting.ViewlintSummary
import com.viewlint.core.reporting.baseline.ViewlintBaseline
import com.viewlint.core.reporting.html.HtmlReportWriter
import com.viewlint.core.reporting.json.JsonReportWriter
import com.viewlint.core.reporting.sarif.SarifReportWriter
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity.RELATIVE
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import java.util.Comparator
import kotlin.io.path.isRegularFile
import kotlin.io.path.pathString

open class ViewlintExtension {
    var enabled: Boolean = true
    var runOnCheck: Boolean = true
    var reportFormats: List<String> = listOf("console", "json", "html", "sarif")
    var outputDir: Any = "build/reports/viewlint"
    var failOn: String = "NEVER"
    var baselineFile: Any? = null
}

@DisableCachingByDefault(because = "Audits project source files and manifests for adaptive risks.")
abstract class ViewlintTask : DefaultTask() {

    @get:InputDirectory
    @get:PathSensitive(RELATIVE)
    abstract val targetProjectDir: DirectoryProperty

    @get:Input
    abstract val reportFormats: ListProperty<String>

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @get:Input
    abstract val failOn: Property<String>

    @get:Optional
    @get:InputFile
    @get:PathSensitive(RELATIVE)
    abstract val baselineFile: RegularFileProperty

    @TaskAction
    fun analyze() {
        val root = targetProjectDir.get().asFile.toPath()
        val formats = reportFormats.get()
        val outDir = outputDirectory.get().asFile
        val gate = failOn.get().uppercase()
        val baseFile = baselineFile.orNull?.asFile

        val manifests = discoverManifests(root)
        val layouts = discoverLayouts(root)
        val kotlinSources = discoverKotlinSources(root)

        val findings = ViewlintEngine(
            rules = ViewlintRuleRegistry.defaultRules()
        ).analyze(
            ViewlintContext(
                projectRoot = root,
                manifests = manifests,
                layouts = layouts,
                kotlinSources = kotlinSources
            )
        )

        // Handle Console Output
        if (formats.any { it.equals("console", ignoreCase = true) }) {
            println()
            println("Viewlint — Android Adaptive Quality Auditor")
            println("============================================")

            findings.forEach { finding ->
                println()
                println("[${finding.severity}] ${finding.ruleId}: ${finding.title}")
                println("File: ${finding.file}")
                println("Evidence: ${finding.evidence}")
                println("Message: ${finding.message}")
                println("Confidence: ${finding.confidence}")
                println("Remediation: ${finding.remediation}")
            }

            println()
            println("--------------------------------------------")
            println("Manifests scanned: ${manifests.size}")
            println("Layouts scanned: ${layouts.size}")
            println("Kotlin sources scanned: ${kotlinSources.size}")
            println("Rules executed: ${ViewlintRuleRegistry.defaultRules().size}")
            println("Analysis complete. Findings: ${findings.size}")
            println("--------------------------------------------")
        }

        // Build Report Model
        val summary = ViewlintSummary(
            totalFindings = findings.size,
            errorCount = findings.count { it.severity == Severity.ERROR },
            warningCount = findings.count { it.severity == Severity.WARNING },
            infoCount = findings.count { it.severity == Severity.INFO },
            byRule = findings.groupingBy { it.ruleId }.eachCount()
        )

        val report = ViewlintReport(
            metadata = ViewlintReportMetadata(
                generatedAt = Instant.now().toString(),
                projectName = targetProjectDir.get().asFile.name
            ),
            summary = summary,
            findings = findings
        )

        outDir.mkdirs()

        // Write JSON Report
        if (formats.any { it.equals("json", ignoreCase = true) }) {
            val jsonFile = File(outDir, "viewlint-report.json")
            jsonFile.writeText(JsonReportWriter.write(report))
            println("Viewlint JSON report written to: ${jsonFile.absolutePath}")
        }

        // Write HTML Report
        if (formats.any { it.equals("html", ignoreCase = true) }) {
            val htmlFile = File(outDir, "viewlint-report.html")
            htmlFile.writeText(HtmlReportWriter.write(report))
            println("Viewlint HTML report written to: ${htmlFile.absolutePath}")
        }

        // Write SARIF Report
        if (formats.any { it.equals("sarif", ignoreCase = true) }) {
            val sarifFile = File(outDir, "viewlint-report.sarif")
            sarifFile.writeText(SarifReportWriter.write(report))
            println("Viewlint SARIF report written to: ${sarifFile.absolutePath}")
        }

        // Baseline & Quality Gate enforcement
        var findingsToEvaluate = findings
        var newFindingsCount = findings.size

        if (baseFile != null) {
            if (baseFile.exists()) {
                val baselineSignatures = ViewlintBaseline.loadBaseline(baseFile)
                val newFindings = ViewlintBaseline.filterNewFindings(findings, baselineSignatures)
                newFindingsCount = newFindings.size
                findingsToEvaluate = newFindings
                println("Baseline loaded from ${baseFile.absolutePath}. Total findings: ${findings.size}, New findings: ${newFindingsCount}")
            } else {
                ViewlintBaseline.saveBaseline(baseFile, findings)
                println("Created new baseline at ${baseFile.absolutePath} with ${findings.size} findings.")
            }
        }

        val hasError = findingsToEvaluate.any { it.severity == Severity.ERROR }
        val hasWarning = findingsToEvaluate.any { it.severity == Severity.WARNING || it.severity == Severity.ERROR }
        val hasNew = newFindingsCount > 0

        val failed = when (gate) {
            "ERROR" -> hasError
            "WARNING" -> hasWarning
            "NEW" -> hasNew
            else -> false
        }

        if (failed) {
            throw GradleException("Viewlint quality gate failed! Threshold '$gate' was exceeded. Findings evaluated: ${findingsToEvaluate.size}")
        }
    }

    private fun discoverManifests(root: Path): List<Path> {
        if (!Files.exists(root) || !Files.isDirectory(root)) {
            return emptyList()
        }

        return try {
            Files.walk(root).use { paths ->
                paths
                    .filter { path ->
                        try {
                            path.isRegularFile() && isAndroidManifest(root, path)
                        } catch (_: Exception) {
                            false
                        }
                    }
                    .sorted()
                    .toList()
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun isAndroidManifest(root: Path, path: Path): Boolean {
        if (path.fileName?.toString() != "AndroidManifest.xml") {
            return false
        }

        val relative = try {
            root.relativize(path)
        } catch (_: Exception) {
            return false
        }

        val hasIgnoredComponent = relative.any { component ->
            val name = component.toString()
            name == "build" || name == ".gradle" || name == ".git" || name == ".idea"
        }
        if (hasIgnoredComponent) {
            return false
        }

        return relative.any { it.toString() == "src" }
    }

    private fun discoverLayouts(root: Path): List<LayoutResource> {
        if (!Files.exists(root) || !Files.isDirectory(root)) {
            return emptyList()
        }

        return try {
            Files.walk(root).use { paths ->
                paths
                    .filter { path ->
                        try {
                            path.isRegularFile() && isLayoutResourceFile(root, path)
                        } catch (_: Exception) {
                            false
                        }
                    }
                    .map { path ->
                        val fileName = path.fileName.toString()
                        val layoutName = fileName.removeSuffix(".xml")
                        val parentName = path.parent?.fileName?.toString() ?: "layout"
                        val qualifier = if (parentName.startsWith("layout")) {
                            parentName
                        } else {
                            "layout"
                        }
                        LayoutResource(
                            name = layoutName,
                            qualifier = qualifier,
                            path = path
                        )
                    }
                    .sorted(Comparator.comparing { it.path.toString() })
                    .toList()
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun isLayoutResourceFile(root: Path, path: Path): Boolean {
        if (path.fileName?.toString()?.endsWith(".xml") != true) {
            return false
        }

        val relative = try {
            root.relativize(path)
        } catch (_: Exception) {
            return false
        }

        val hasIgnoredComponent = relative.any { component ->
            val name = component.toString()
            name == "build" || name == ".gradle" || name == ".git" || name == ".idea"
        }
        if (hasIgnoredComponent) {
            return false
        }

        val parentDir = path.parent?.fileName?.toString() ?: return false
        return parentDir.startsWith("layout")
    }

    private fun discoverKotlinSources(root: Path): List<Path> {
        if (!Files.exists(root) || !Files.isDirectory(root)) {
            return emptyList()
        }

        return try {
            Files.walk(root).use { paths ->
                paths
                    .filter { path ->
                        try {
                            path.isRegularFile() && isKotlinSourceFile(root, path)
                        } catch (_: Exception) {
                            false
                        }
                    }
                    .sorted(Comparator.comparing { it.pathString })
                    .toList()
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun isKotlinSourceFile(root: Path, path: Path): Boolean {
        val fileName = path.fileName?.toString() ?: return false
        if (!fileName.endsWith(".kt")) {
            return false
        }

        val relative = try {
            root.relativize(path)
        } catch (_: Exception) {
            return false
        }

        val pathString = relative.toString()
        if (pathString.contains("build") ||
            pathString.contains(".gradle") ||
            pathString.contains(".git") ||
            pathString.contains(".idea") ||
            pathString.contains("generated")) {
            return false
        }

        return relative.any { it.toString() == "src" } || relative.any { it.toString() == "kotlin" || it.toString() == "java" }
    }
}

class ViewlintPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        val extension = project.extensions.create(
            "viewlint",
            ViewlintExtension::class.java
        )

        val taskProvider = project.tasks.register(
            "viewlint",
            ViewlintTask::class.java
        ) { task ->
            task.group = "verification"
            task.description = "Audits Android projects for adaptive UI risks."
            task.targetProjectDir.set(project.layout.projectDirectory)
            task.enabled = extension.enabled
            task.reportFormats.set(extension.reportFormats)
            task.outputDirectory.set(project.file(extension.outputDir))
            task.failOn.set(extension.failOn)
            if (extension.baselineFile != null) {
                task.baselineFile.set(project.file(extension.baselineFile!!))
            }
        }

        project.afterEvaluate {
            taskProvider.configure { task ->
                task.enabled = extension.enabled
                task.reportFormats.set(extension.reportFormats)
                task.outputDirectory.set(project.file(extension.outputDir))
                task.failOn.set(extension.failOn)
                if (extension.baselineFile != null) {
                    task.baselineFile.set(project.file(extension.baselineFile!!))
                }
            }

            if (extension.enabled && extension.runOnCheck) {
                val checkTask = project.tasks.findByName("check")
                if (checkTask != null) {
                    checkTask.dependsOn(taskProvider)
                } else {
                    project.tasks.register("check") { check ->
                        check.group = "verification"
                        check.description = "Verification lifecycle task."
                        check.dependsOn(taskProvider)
                    }
                }
            }
        }
    }
}
