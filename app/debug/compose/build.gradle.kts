plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlinx.serialization)
}

kotlin {
    jvm("desktop")

    listOf(
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
            implementation(libs.compose.runtime)
            api(projects.app.common.composeApp)
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

compose.desktop {
    configureJvmAppDistribution()
}
