plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
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
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
    lint {
        abortOnError = false
        warningsAsErrors = false
        checkDependencies = true
        checkReleaseBuilds = true
        lintConfig = file("$rootDir/lint.xml")
        htmlReport = true
        sarifReport = true
        textReport = true
    }
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
    implementation(libs.hilt.android)
    implementation(libs.androidx.hilt.navigation.compose)
    ksp(libs.hilt.compiler)
    annotationProcessor(libs.kotlin.metadata.jvm)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
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
}

tasks.register("codeQuality") {
    group = "verification"
    description = "Ejecuta Android Lint + ktlint + detekt en un solo comando."
    dependsOn("ktlintCheck", "detekt", "lint")
}

listOf("ktlintCheck", "detekt", "lint").forEach { check ->
    tasks.named(check) { mustRunAfter("ktlintFormat") }
}

tasks.register("formatAndAnalyze") {
    group = "verification"
    description = "Formatea el codigo (ktlintFormat) y luego verifica todo (ktlintCheck + detekt + lint)."
    dependsOn("ktlintFormat", "codeQuality")
}
