plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlinx.serialization)
}

kotlin {
    android {
        configureComposeAndroidApp(project, isDebug = false)
    }

    dependencies {
        implementation(projects.app.release.compose)
    }
}