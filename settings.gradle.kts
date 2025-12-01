rootProject.name = "blean"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
        mavenLocal()
        maven("https://jogamp.org/deployment/maven") // for webview, to remove later mb
        maven("https://s01.oss.sonatype.org/content/repositories/snapshots/")
        maven("https://jitpack.io")
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        mavenLocal()
        maven("https://jogamp.org/deployment/maven") // for webview, to remove later mb
        maven("https://s01.oss.sonatype.org/content/repositories/snapshots/")
        maven("https://jitpack.io")
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}

fun collectGradleProjects(rootDir: File): List<String> {
    val projects = mutableListOf<String>()

    fun walk(dir: File, path: String) {
        val hasBuild = File(dir, "build.gradle.kts").exists()
        val hasSettings = File(dir, "settings.gradle.kts").exists()

        if (hasBuild && !hasSettings) {
            projects += path
        }

        dir.listFiles()
            ?.filter { it.isDirectory }
            ?.forEach { child ->
                walk(child, "$path:${child.name}")
            }
    }

    rootDir.listFiles()
        ?.filter { it.isDirectory && it.name !in setOf("buildSrc", "build-logic") }
        ?.forEach { dir -> walk(dir, ":${dir.name}") }

    return projects
}

collectGradleProjects(rootDir).forEach {
    include(it)
}