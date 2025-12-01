plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    jvm()
    androidTarget()
    iosX64()
    iosArm64()
    iosSimulatorArm64()
    mingwX64()
    macosX64()
    macosArm64()
    linuxX64()
    linuxArm64()
    tvosArm64()
    tvosX64()
    watchosArm32()
    watchosArm64()
    watchosX64()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(compose.runtime)

                implementation(libs.kotlinx.datetime)
                implementation(libs.ktor.clientSerializationJson)
                api(libs.kotlinResult)
            }
        }

        val jvmMain by getting {
            dependencies {
                implementation(libs.kotlinx.coroutines.swing)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(libs.kotlin.test)
//                implementation(libs.turbine)
                implementation(libs.kotlinx.coroutines.test)
            }
        }
    }
}

android {
    configureAndroidLibrary(project, compose = false)
}