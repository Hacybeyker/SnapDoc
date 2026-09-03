plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
    alias(libs.plugins.room)
    alias(libs.plugins.kover)
    alias(libs.plugins.roborazzi)
}

android {
    namespace = "com.hacybeyker.snapdoc"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.hacybeyker.snapdoc"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = libs.versions.appVersion.get()
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
    testOptions {
        unitTests {
            // Robolectric inflates the real resources — strings, themes, densities — on the JVM,
            // which is what lets a screenshot test render the app's actual look and not a stub.
            isIncludeAndroidResources = true
            all { test ->
                test.maxHeapSize = "2g"
            }
        }
    }
    lint {
        // The gate is only a gate if it can fail: a lint error breaks the build, warnings do not.
        abortOnError = true
        warningsAsErrors = false
        checkDependencies = true
        checkReleaseBuilds = true
        lintConfig = file("$rootDir/lint.xml")
        htmlReport = true
        sarifReport = true
        textReport = true
    }
}

// Exported schemas are what make a versioned migration reviewable; without them Room cannot
// diff versions and `fallbackToDestructiveMigration` becomes the tempting shortcut.
room {
    schemaDirectory("$projectDir/schemas")
}

composeCompiler {
    stabilityConfigurationFiles.add(rootProject.layout.projectDirectory.file("compose_stability.conf"))
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.kotlinx.serialization.core)
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.compose)
    implementation(libs.play.services.mlkit.document.scanner)
    implementation(libs.mlkit.text.recognition)
    implementation(libs.mlkit.genai.prompt)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.hilt.android)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    ksp(libs.hilt.compiler)
    annotationProcessor(libs.kotlin.metadata.jvm)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.androidx.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    // Not for instrumented tests — there are none. It is the debug manifest entry for
    // ComponentActivity that createComposeRule launches, so removing it fails every golden with
    // "Unable to resolve activity".
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}

// The scaffolding removes the sample tests on init; without this, Gradle fails
// when the test source set exists (MainDispatcherRule) but there's no @Test yet.
tasks.withType<Test>().configureEach {
    failOnNoDiscoveredTests = false
}

detekt {
    buildUponDefaultConfig = true
    parallel = true
    config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
    basePath = rootDir
}

ktlint {
    android.set(true)
    ignoreFailures.set(false)
    reporters {
        // PLAIN keeps the violations readable in the console and in the .txt report; CHECKSTYLE is the
        // only format SonarCloud reads ktlint findings from. Declaring reporters replaces the default,
        // so leaving PLAIN out would silently make a local failure say only "see the reports".
        reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.PLAIN)
        reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.CHECKSTYLE)
    }
}

tasks.register("codeQuality") {
    group = "verification"
    description = "Runs Android Lint + ktlint + detekt in a single command."
    dependsOn("ktlintCheck", "detekt", "lint")
}

listOf("ktlintCheck", "detekt", "lint").forEach { check ->
    tasks.named(check) { mustRunAfter("ktlintFormat") }
}

tasks.register("formatAndAnalyze") {
    group = "verification"
    description = "Formats the code (ktlintFormat), then verifies everything (ktlintCheck + detekt + lint)."
    dependsOn("ktlintFormat", "codeQuality")
}

roborazzi {
    // The goldens live in the repository, not in build/: verifyRoborazziDebug diffs against them and
    // recordRoborazziDebug re-baselines after a change that was meant to happen. A screenshot that is
    // not committed proves nothing on someone else's machine.
    outputDir.set(file("src/test/screenshots"))
}

sonar {
    properties {
        property("sonar.androidLint.reportPaths", "build/reports/lint-results-debug.xml")
        property("sonar.kotlin.detekt.reportPaths", "build/reports/detekt/detekt.xml")
        property(
            "sonar.kotlin.ktlint.reportPaths",
            listOf(
                "build/reports/ktlint/ktlintKotlinScriptCheck/ktlintKotlinScriptCheck.xml",
                "build/reports/ktlint/ktlintMainSourceSetCheck/ktlintMainSourceSetCheck.xml",
                "build/reports/ktlint/ktlintTestSourceSetCheck/ktlintTestSourceSetCheck.xml",
                "build/reports/ktlint/ktlintAndroidTestSourceSetCheck/ktlintAndroidTestSourceSetCheck.xml"
            ).joinToString(",")
        )
        property("sonar.coverage.jacoco.xmlReportPaths", "build/reports/kover/reportDebug.xml")
        // Coverage is claimed only where it can be earned on the JVM. Everything below talks to
        // CameraX, Play services, ML Kit, Room or the file system, and is verified on a device.
        property(
            "sonar.coverage.exclusions",
            listOf(
                "**/ui/**",
                "**/navigation/**",
                "**/core/database/**",
                "**/di/**",
                "**/*Module.kt",
                "**/MainActivity.kt",
                "**/MainApplication.kt",
                "**/feature/camera/data/**",
                "**/feature/ocr/data/GeminiNanoDocumentAnalyzer.kt",
                "**/feature/ocr/data/MlKitDocumentTextRecognizer.kt",
                "**/feature/library/data/RoomDocumentRepository.kt",
                "**/feature/library/data/PdfDocumentExporter.kt"
            ).joinToString(",")
        )
    }
}

kover {
    reports {
        filters {
            includes {
                classes(
                    "com.hacybeyker.snapdoc.*.domain.*",
                    "com.hacybeyker.snapdoc.*.data.*",
                    "com.hacybeyker.snapdoc.*ViewModel*"
                )
            }
            excludes {
                classes(
                    "*_Impl",
                    "*_Impl$*",
                    "*_Factory",
                    "*_Factory$*",
                    "*Module",
                    "*Module$*",
                    "*Module_*",
                    "*_HiltModules*"
                )
                // The platform boundary: none of these can run without a device, so counting them
                // would only produce a number that has to be argued away.
                classes(
                    "com.hacybeyker.snapdoc.feature.camera.data.*",
                    "com.hacybeyker.snapdoc.feature.ocr.data.GeminiNanoDocumentAnalyzer*",
                    "com.hacybeyker.snapdoc.feature.ocr.data.MlKitDocumentTextRecognizer*",
                    "com.hacybeyker.snapdoc.feature.library.data.RoomDocumentRepository*",
                    "com.hacybeyker.snapdoc.feature.library.data.PdfDocumentExporter*"
                )
            }
        }
        verify {
            rule("Line coverage of measured classes (domain, data, ViewModels)") {
                minBound(90)
            }
        }
    }
}
