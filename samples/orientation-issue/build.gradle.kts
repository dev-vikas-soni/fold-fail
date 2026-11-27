plugins {
    id("io.github.dev-vikas-soni.foldfail")
}

foldFail {
    reportFormats = listOf("console", "json", "html", "sarif")
}
