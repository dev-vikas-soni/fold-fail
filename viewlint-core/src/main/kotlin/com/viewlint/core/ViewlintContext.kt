package com.viewlint.core

import java.nio.file.Path

data class LayoutResource(
    val name: String,
    val qualifier: String,
    val path: Path
)

data class ViewlintContext(
    val projectRoot: Path,
    val manifests: List<Path>,
    val layouts: List<LayoutResource> = emptyList(),
    val kotlinSources: List<Path> = emptyList()
)
