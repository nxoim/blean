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
    androidTarget()
    iosX64()
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
                implementation(compose.runtime)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.okio)
                implementation(libs.okioFakeFileSystem)
                api(libs.kotlinResult)

                implementation(libs.kotlinx.datetime)
                implementation(libs.ktor.clientSerializationJson)

                implementation(libs.roomRuntime)

                implementation(projects.commonThingsDumpster)
                implementation(libs.bundledSqlDriver)
                implementation(projects.models)
                implementation(projects.platformCredentialsManagement)
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

dependencies {
    ksp(libs.roomCompiler)
//    add("kspCommonMainMetadata", libs.roomCompiler)
}

android {
    configureAndroidLibrary(project, compose = false)
}