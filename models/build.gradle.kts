plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.ksp)
}

kotlin {
    jvm()
    androidLibrary {
        configureAndroidLibrary(project)
    }
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        val androidMain by getting {
            dependencies {
                implementation(libs.androidx.startup)
            }
        }

        val commonMain by getting {
            dependencies {
                implementation(libs.compose.runtime)

                implementation(libs.kotlinx.datetime)
                implementation(libs.ktor.clientSerializationJson)

                implementation(projects.commonThingsDumpster)
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