import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlinx.serialization)
}

val composeAppFrameworkName = "ComposeApp"

kotlin {
    jvm("desktop")
    androidLibrary {
        configureAndroidAppAsLibrary(project)
    }
    val xcf = XCFramework(composeAppFrameworkName)

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = composeAppFrameworkName
            isStatic = true
            binaryOption("bundleId", "$applicationId.${composeAppFrameworkName}")
            xcf.add(this)

            export(projects.client)
            export(projects.platformCredentialsManagement)
            export(libs.decompose)
            export(libs.essentyLifecycle)
            export(libs.essentyStateKeeper)
            linkerOpts.add("-dead_strip")
            linkerOpts.add("LLVM_LTO=FullLTO")
        }
    }

    sourceSets {
        val desktopMain by getting

        androidMain.dependencies {
            api(libs.androidx.activity.compose)
            implementation(libs.ktor.clientCio)
            implementation(libs.compose.uiTooling)
        }
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.androidx.lifecycle.runtime.compose)
            implementation(libs.androidxCollection)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.animationGraphics)
            implementation(libs.compose.preview)
//            implementation(libs.androidx.lifecycle.viewmodel)
//            implementation(libs.androidx.lifecycle.runtime.compose)
//            implementation(project(":ui-components"))
            api(projects.client)

//            implementation(libs.ktor.clientCore)
//            implementation(libs.ktor.clientLogging)
//            implementation(libs.ktor.clientSerializationJson)
//            implementation(libs.ktor.clientContentnegotiation)

            implementation(libs.filekit)

            implementation(libs.coil.network.ktor)
            implementation(libs.coil.compose)

//            implementation(libs.koin.compose)

            api(libs.decompose)
            implementation(libs.decomposeComposeExtensions)
            implementation(libs.decomposeJetpackExtensions)

//            implementation(libs.haze)
//            implementation(libs.hazeMaterials)
            implementation(libs.kotlinx.datetime)
            implementation(libs.webviewThing)

            implementation(libs.evolpaginkCompose)

            implementation(projects.commonThingsDumpster)
            implementation(projects.composeVideoPlayer)

            implementation(libs.composeGraphicsShapes)
            implementation(libs.compose.materialIconsExtended)
            implementation(projects.app.common.parts.ui.commons)
            implementation(projects.app.common.parts.ui.screens.content)
            implementation(projects.app.common.parts.ui.screens.authentication.login)
        }
        desktopMain.dependencies {
            implementation(libs.kotlinx.coroutines.swing)
            implementation(libs.ktor.clientCio)
            implementation(libs.compose.preview)
        }
        iosMain.dependencies {
            api(libs.essentyLifecycle)
            api(libs.essentyStateKeeper)
            api(libs.decompose)
            implementation(libs.ktor.clientDarwin)
        }
    }
}

generateDefaultBuildConfigAndSetModuleAsReceiver()
