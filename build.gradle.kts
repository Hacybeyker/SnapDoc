// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.sonarqube)
}

// SonarCloud reads what the local gate already produces — Lint, detekt, ktlint and Kover reports —
// rather than re-running its own analysis, so a finding on the dashboard is one the build can also
// show you offline.
sonar {
    properties {
        property("sonar.projectKey", "com.hacybeyker.snapdoc")
        property("sonar.organization", "hacybeyker")
        property("sonar.projectName", "app-snapdoc-android")
        property("sonar.host.url", "https://sonarcloud.io")
        property(
            "sonar.projectDescription",
            "Android document scanner that reads and understands documents entirely on-device: " +
                "CameraX capture, ML Kit scanning and OCR, Gemini Nano insights, a searchable " +
                "archive and PDF export."
        )
        property("sonar.projectVersion", libs.versions.appVersion.get())
        property("sonar.sourceEncoding", "UTF-8")
        property("sonar.exclusions", "**/*.webp,**/*.png,**/*.jar")
        // The job fails when the Quality Gate does: a dashboard nobody is forced to read is a
        // dashboard nobody reads.
        property("sonar.qualitygate.wait", "true")
    }
}

tasks.named("sonar") {
    dependsOn(":app:lint", ":app:detekt", ":app:ktlintCheck", ":app:koverXmlReportDebug")
}
