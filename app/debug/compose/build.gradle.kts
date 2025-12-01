plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlinx.serialization)
}

kotlin {
    jvm("desktop")
    androidTarget()

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeDebugApp"
            isStatic = true
            export(projects.app.common.composeApp)
        }
    }

    sourceSets {
        commonMain.dependencies {
            // needed to avoid androidx.compose.compiler.plugins.kotlin.IncompatibleComposeRuntimeVersionException
            implementation(compose.runtime)
            implementation(projects.app.common.composeApp)
        }

        iosMain.dependencies {
            api(projects.app.common.composeApp)
        }
    }
}

generateAppBuildConfig(
    isDebug = true,
    verboseLogs = true
)

android {
    configureComposeAndroidApp(project, isDebug = true)
}

compose.desktop {
    configureJvmAppDistribution()
}
