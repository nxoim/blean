plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
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
    )

    sourceSets {
        val desktopMain by getting

        androidMain.dependencies {
            api(libs.androidx.activity.compose)
            implementation(compose.uiTooling)
        }
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.animationGraphics)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.filekit)

            implementation(libs.coil.network.ktor)
            implementation(libs.coil.compose)

//            implementation(libs.haze)
//            implementation(libs.hazeMaterials)
            implementation(libs.kotlinx.datetime)

            implementation(libs.evolpaginkCompose)

            implementation(projects.commonThingsDumpster)
            implementation(projects.composeVideoPlayer)
            implementation(projects.app.common.parts.postRelatedCommons)

            implementation(libs.composeGraphicsShapes)
            implementation(compose.materialIconsExtended)
            implementation(libs.decompose)
        }
        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutines.swing)
            implementation(compose.preview)
        }
    }
}

android {
    configureAndroidLibrary(project, compose = true)
}
