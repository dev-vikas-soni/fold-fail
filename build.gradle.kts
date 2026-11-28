plugins {
    id("org.jetbrains.kotlin.jvm") version "2.3.20" apply false
}

allprojects {
    group = "io.github.dev-vikas-soni"
    version = "1.0.0"

    plugins.withId("org.jetbrains.kotlin.jvm") {
        tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
            compilerOptions {
                jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
            }
        }
        tasks.withType<JavaCompile>().configureEach {
            options.release.set(21)
        }
    }
}

tasks.register("viewlint") {
    group = "verification"
    description = "Audits project source files and manifests for adaptive risks."
    dependsOn(":viewlint-gradle-plugin:shadowJar")

    doLast {
        val pluginJarProvider = project(":viewlint-gradle-plugin").tasks.named("shadowJar", org.gradle.api.tasks.bundling.Jar::class.java)
        val pluginJar = pluginJarProvider.get().archiveFile.get().asFile

        val classLoader = java.net.URLClassLoader(
            arrayOf(pluginJar.toURI().toURL()),
            javaClass.classLoader
        )

        val contextClass = classLoader.loadClass("com.viewlint.core.ViewlintContext")
        val engineClass = classLoader.loadClass("com.viewlint.core.ViewlintEngine")
        val registryClass = classLoader.loadClass("com.viewlint.analyzer.ViewlintRuleRegistry")
        val jsonWriterClass = classLoader.loadClass("com.viewlint.core.reporting.json.JsonReportWriter")
        val htmlWriterClass = classLoader.loadClass("com.viewlint.core.reporting.html.HtmlReportWriter")
        val sarifWriterClass = classLoader.loadClass("com.viewlint.core.reporting.sarif.SarifReportWriter")

        val instanceField = registryClass.getField("INSTANCE")
        val registryInstance = instanceField.get(null)
        val rules = registryClass.getMethod("defaultRules").invoke(registryInstance) as List<*>

        val rootPath = project.layout.projectDirectory.asFile.toPath()
        val manifests = mutableListOf<java.nio.file.Path>()

        java.nio.file.Files.walk(rootPath).use { paths ->
            paths.filter { p ->
                val fileName = p.fileName?.toString()
                val pathString = p.toString().replace("\\", "/")
                fileName == "AndroidManifest.xml" &&
                !pathString.contains("/build/") &&
                !pathString.contains("/.gradle/") &&
                !pathString.contains("/.git/") &&
                !pathString.contains("/.idea/")
            }.forEach { manifests.add(it) }
        }

        val contextConstructor = contextClass.getConstructor(
            java.nio.file.Path::class.java,
            List::class.java,
            List::class.java,
            List::class.java
        )
        val context = contextConstructor.newInstance(rootPath, manifests, emptyList<Any>(), emptyList<Any>())

        val engine = engineClass.getConstructor(List::class.java).newInstance(rules)
        val findings = engineClass.getMethod("analyze", contextClass).invoke(engine, context) as List<*>

        val reportClass = classLoader.loadClass("com.viewlint.core.reporting.ViewlintReport")
        val metadataClass = classLoader.loadClass("com.viewlint.core.reporting.ViewlintReportMetadata")
        val summaryClass = classLoader.loadClass("com.viewlint.core.reporting.ViewlintSummary")
        val severityClass = classLoader.loadClass("com.viewlint.core.Severity")

        @Suppress("UNCHECKED_CAST")
        val errorSeverity = java.lang.Enum.valueOf(severityClass as Class<out Enum<*>>, "ERROR")
        @Suppress("UNCHECKED_CAST")
        val warningSeverity = java.lang.Enum.valueOf(severityClass as Class<out Enum<*>>, "WARNING")
        @Suppress("UNCHECKED_CAST")
        val infoSeverity = java.lang.Enum.valueOf(severityClass as Class<out Enum<*>>, "INFO")

        var errorCount = 0
        var warningCount = 0
        var infoCount = 0
        val byRule = mutableMapOf<String, Int>()

        val findingClass = classLoader.loadClass("com.viewlint.core.ViewlintFinding")
        val getSeverity = findingClass.getMethod("getSeverity")
        val getRuleId = findingClass.getMethod("getRuleId")

        for (finding in findings) {
            val sev = getSeverity.invoke(finding)
            val rId = getRuleId.invoke(finding) as String
            if (sev == errorSeverity) errorCount++
            else if (sev == warningSeverity) warningCount++
            else if (sev == infoSeverity) infoCount++
            byRule[rId] = (byRule[rId] ?: 0) + 1
        }

        val summary = summaryClass.getConstructor(
            Int::class.javaPrimitiveType,
            Int::class.javaPrimitiveType,
            Int::class.javaPrimitiveType,
            Int::class.javaPrimitiveType,
            Map::class.java
        ).newInstance(findings.size, errorCount, warningCount, infoCount, byRule)

        val metadata = metadataClass.getConstructor(
            String::class.java,
            String::class.java,
            String::class.java,
            String::class.java,
            String::class.java
        ).newInstance("1.0", "Viewlint", project.version.toString(), java.time.Instant.now().toString(), project.name)

        val report = reportClass.getConstructor(metadataClass, summaryClass, List::class.java)
            .newInstance(metadata, summary, findings)

        val outDir = project.file("build/reports/viewlint")
        outDir.mkdirs()

        val jsonWriterInstance = jsonWriterClass.getField("INSTANCE").get(null)
        val htmlWriterInstance = htmlWriterClass.getField("INSTANCE").get(null)
        val sarifWriterInstance = sarifWriterClass.getField("INSTANCE").get(null)

        val jsonText = jsonWriterClass.getMethod("write", reportClass).invoke(jsonWriterInstance, report) as String
        val htmlText = htmlWriterClass.getMethod("write", reportClass).invoke(htmlWriterInstance, report) as String
        val sarifText = sarifWriterClass.getMethod("write", reportClass).invoke(sarifWriterInstance, report) as String

        java.io.File(outDir, "viewlint-report.json").writeText(jsonText)
        java.io.File(outDir, "viewlint-report.html").writeText(htmlText)
        val sarifFile = java.io.File(outDir, "viewlint-report.sarif")
        sarifFile.writeText(sarifText)

        println("Viewlint audit complete. Manifests scanned: ${manifests.size}, Findings: ${findings.size}")
        println("Viewlint SARIF report written to: ${sarifFile.absolutePath}")
    }
}
