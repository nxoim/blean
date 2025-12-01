import com.android.build.gradle.LibraryExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.get

inline fun LibraryExtension.configureAndroidAppAsLibrary(project: Project) =
    configureAndroidLibrary(project, compose = true, namespace =  applicationId)

inline fun LibraryExtension.configureAndroidLibrary(
    project: Project,
    compose: Boolean,
    namespace: String = with(project) { "$applicationId.$name" },
) {
    this@configureAndroidLibrary.namespace = namespace
    compileSdk = AndroidBuildStuff.SDK.TARGET

    sourceSets["main"].resources.srcDirs("src/commonMain/resources")

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    buildTypes {
        release {
            consumerProguardFile("consumer-proguard-rules.pro")
        }
    }

    return buildFeatures {
        this.compose = compose
    }
}