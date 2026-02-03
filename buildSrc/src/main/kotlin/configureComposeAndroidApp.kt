import com.android.build.api.dsl.ApplicationExtension
import com.android.build.gradle.internal.dsl.BaseAppModuleExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.get

inline fun ApplicationExtension.configureComposeAndroidApp(
    project: Project,
    isDebug: Boolean
) = configureComposeAndroidApp(
    project,
    applicationId,
    baseAppVersionCode,
    baseAppVersionString,
    isDebug
)

inline fun ApplicationExtension.configureComposeAndroidApp(
    project: Project,
    applicationId: String,
    versionCode: Int,
    versionName: String,
    isDebug: Boolean
) = with(project) {
    setupAndroidAppSigning(project, signConfigId)
    namespace = applicationId

    compileSdk = AndroidBuildStuff.SDK.TARGET

    defaultConfig {
        minSdk = AndroidBuildStuff.SDK.MIN
        targetSdk = AndroidBuildStuff.SDK.TARGET

        this.applicationId = applicationId
        this.versionCode = versionCode
        this.versionName = versionName
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    buildTypes {
        if (isDebug) {
            release {
                isMinifyEnabled = true
                isShrinkResources = true
                isDebuggable = true
                isProfileable = true
                signingConfig = signingConfigs.getByName(signConfigId)
            }

            debug {
                signingConfig = signingConfigs.getByName(signConfigId)
            }
        } else {
            release {
                isMinifyEnabled = true
                isShrinkResources = true
                proguardFiles(
                    getDefaultProguardFile("proguard-android-optimize.txt"),
                    "proguard-rules.pro"
                )
                signingConfig = signingConfigs.getByName(signConfigId)
            }

            debug {
                signingConfig = signingConfigs.getByName(signConfigId)
            }
        }
    }
    buildFeatures {
        compose = true
    }
}

const val signConfigId = "signed"