plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    android {
        configureComposeAndroidApp(project, isDebug = true)
    }

    dependencies {
        implementation(projects.app.debug.compose)
    }
}