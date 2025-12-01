plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinx.serialization)
}

kotlin {
    jvm()
    androidTarget()
    iosX64()
    iosArm64()
    iosSimulatorArm64()
    linuxX64()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.ktor.clientSerializationJson)
                api(libs.kotlinResult)
                api(libs.kermitLogger)
                api(libs.concurrentCollections)
                api(projects.bskyPrimitives)
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
    }
}

android {
    configureAndroidLibrary(project, compose = false)
}
