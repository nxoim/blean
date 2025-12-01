import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.jetbrainsCompose) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinx.serialization) apply false
    alias(libs.plugins.ksp) apply false
//    alias(libs.plugins.room) apply false
}

subprojects {
    afterEvaluate {
        plugins.withId(libs.plugins.kotlinMultiplatform.get().pluginId) {
            extensions.configure<KotlinMultiplatformExtension> {
                jvmToolchain(21)

                targets.all {
                    compilations.all {
                        this@configure.compilerOptions {
                            freeCompilerArgs.addAll(
                                "-Xcontext-parameters",
                                "-Xcontext-sensitive-resolution"
                            )
                        }
                    }
                }

                sourceSets {
                    all {
                        languageSettings {
                            optIn("org.jetbrains.compose.resources.ExperimentalResourceApi")
                        }
                    }
                }
            }
        }
    }
}
