plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    jvm()
    androidTarget {
        publishLibraryVariants("release")
    }
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        val androidMain by getting {
            dependencies {
                implementation(libs.androidx.activity.compose)
//                implementation(libs.ktor.clientCio)
                implementation(libs.androidxMedia3Exoplayer)
                implementation(libs.androidxMedia3Ui)
                implementation(libs.androidxMedia3CommonKtx)
                implementation(libs.androidxMedia3Hls)
            }
        }
        val commonMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.ui)
                implementation(compose.components.resources)
//                implementation(libs.ktor.clientSerializationJson)
                implementation(libs.kotlinx.datetime)
                api(libs.kotlinResult)
                implementation(libs.concurrentCollections)
                api(projects.commonThingsDumpster)
                implementation(libs.androidx.lifecycle.runtime.compose)
                implementation(libs.kermitLogger)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.kotlinx.coroutines.test)
                implementation(libs.kotlinx.datetime)
                implementation(libs.okio)
            }
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutines.swing)
//            implementation(libs.ktor.clientCio)
            implementation(compose.preview)
        }
        appleMain.dependencies {
//            implementation(libs.ktor.clientDarwin)
        }
    }
}

android {
    configureAndroidLibrary(project, compose = true)
}