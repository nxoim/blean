plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinx.serialization)
}

kotlin {
    jvm()
    androidLibrary {
        configureAndroidLibrary(project)
    }

    iosArm64()
    iosSimulatorArm64()
    linuxX64()

    sourceSets {
        val androidMain by getting {
            dependencies {
                implementation(libs.whyolegCryptography.jvm)
                api(libs.ktor.clientOkhttp)
            }
        }
        val commonMain by getting {
            dependencies {
                api(libs.ktor.clientCore)
                api(libs.ktor.clientLogging)
                implementation(libs.ktor.clientSerializationJson)
                implementation(libs.ktor.clientContentnegotiation)
                implementation(libs.whyolegCryptography.core)
                api(libs.kotlinResult)
                implementation(projects.commonThingsDumpster)
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
            implementation(libs.whyolegCryptography.jvm)
            api(libs.ktor.clientCio)
        }
        appleMain.dependencies {
            implementation(libs.whyolegCryptography.apple)
            api(libs.ktor.clientDarwin)
        }
    }
}