plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlinx.serialization)
}

kotlin {
    jvm("desktop")
    androidLibrary {
        configureAndroidLibrary(project)
    }
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    )

    sourceSets {
        val desktopMain by getting

        androidMain.dependencies {
            api(libs.androidx.activity.compose)
            implementation(libs.compose.uiTooling)
        }
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.animationGraphics)
            implementation(libs.compose.preview)
            implementation(libs.filekit)

            implementation(libs.coil.network.ktor)
            implementation(libs.coil.compose)

//            implementation(libs.haze)
//            implementation(libs.hazeMaterials)
            implementation(libs.kotlinx.datetime)

            implementation(libs.evolpaginkCompose)

            implementation(projects.commonThingsDumpster)
            implementation(projects.composeVideoPlayer)

            implementation(libs.composeGraphicsShapes)
            implementation(libs.compose.materialIconsExtended)

            implementation(projects.app.common.parts.ui.commons)
            implementation(libs.webviewThing)
        }
        desktopMain.dependencies {
            implementation(libs.kotlinx.coroutines.swing)
            implementation(libs.compose.preview)
        }
    }
}