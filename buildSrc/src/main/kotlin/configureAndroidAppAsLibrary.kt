@file:Suppress("UnstableApiUsage")

import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.creating
import org.gradle.kotlin.dsl.get

inline fun KotlinMultiplatformAndroidLibraryTarget.configureAndroidAppAsLibrary(project: Project) =
    configureAndroidLibrary(project, namespace =  applicationId)

inline fun KotlinMultiplatformAndroidLibraryTarget.configureAndroidLibrary(
    project: Project,
    namespace: String = with(project) { "$applicationId.$name" },
) {
    this@configureAndroidLibrary.namespace = namespace
    compileSdk { version = release(AndroidBuildStuff.SDK.TARGET) }
    androidResources { enable = true }

//    sourceSets["main"].resources.srcDirs("src/commonMain/resources")

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    optimization {
        consumerKeepRules.apply {
            publish = true
            file("consumer-proguard-rules.pro")
        }
    }

    compilations.getByName("main") {
        this.compileTaskProvider.configure {
            compilerOptions {

            }
        }
    }

    withHostTest {
        isIncludeAndroidResources = true
    }

    // Opt-in to enable and configure device-side (instrumented) tests
    withDeviceTest {
        instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        execution = "HOST"
    }
}