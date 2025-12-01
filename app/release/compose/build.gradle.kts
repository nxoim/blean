import org.gradle.internal.os.OperatingSystem.current

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlinx.serialization)
}

kotlin {
    jvm("desktop")
    androidTarget()

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
            export(projects.client)
            export(libs.decompose)
            export(libs.essentyLifecycle)
            export(libs.essentyStateKeeper)
        }
    }

    sourceSets {
        commonMain.dependencies {
            // needed to avoid androidx.compose.compiler.plugins.kotlin.IncompatibleComposeRuntimeVersionException
            implementation(compose.runtime)
            implementation(projects.app.common.composeApp)
        }

        iosMain.dependencies {
            api(libs.essentyLifecycle)
            api(libs.essentyStateKeeper)
            api(libs.decompose)
            implementation(libs.ktor.clientDarwin)
        }
    }
}

generateAppBuildConfig(
    isDebug = false,
    verboseLogs = false
)

android {
    configureComposeAndroidApp(project, isDebug = false)
}

compose.desktop {
    configureJvmAppDistribution()
}

tasks {
    if (current().isMacOsX) {
        val entryFrameworkName = "ComposeCommonAppEntry"
        val xcodeProjectPath = "../../common/iosApp/$entryFrameworkName.xcodeproj"
        val xcodeSchemeName = "stub app"
        val releaseConfig = "Release"
        val composeAppFramework = "ComposeApp.xcframework"

        val buildFolderInSharedXcodeProject = layout.buildDirectory.dir("../../../common/iosApp/build")
        val deviceXCArchiveFolder =
            buildFolderInSharedXcodeProject.map { it.file("${xcodeSchemeName}_Device.xcarchive").asFile }
        val simulatorXCArchiveFolder =
            buildFolderInSharedXcodeProject.map { it.file("${xcodeSchemeName}_Simulator.xcarchive").asFile }
        val combinedArchiveFolder =
            buildFolderInSharedXcodeProject.map { it.dir("${entryFrameworkName}.xcframework").asFile }

        val buildComposeAppXCFramework = getByPath(":app:common:composeApp:assembleComposeAppReleaseXCFramework")

        val cleanXcodeBuild by registering(Delete::class) {
            group = "build"
            description = "Cleans the intermediate Xcode build directory."
            delete(buildFolderInSharedXcodeProject)
        }

        val cleanXCFramework by registering(Delete::class) {
            group = "build"
            description = "Removes existing ${entryFrameworkName}.xcframework output."
            delete(combinedArchiveFolder)
        }

        named("clean") {
            dependsOn(cleanXcodeBuild, cleanXCFramework)
        }

        val commonXcodeFlags = listOf(
            "-project", xcodeProjectPath,
            "-scheme", xcodeSchemeName,
            "-configuration", releaseConfig,
            "SKIP_INSTALL=NO",
            "BUILD_LIBRARY_FOR_DISTRIBUTION=YES",
            "CODE_SIGNING_REQUIRED=NO",
            "CODE_SIGNING_ALLOWED=NO"
        )
        val copyComposeAppFrameworkFromBuildToShared by registering(Copy::class) {
            group = "build setup"
            description = "Copy $composeAppFramework from build into the shared module's iosApp/Frameworks"
            dependsOn(buildComposeAppXCFramework)
            val frameworkFolderInComposeAppBuildCache =
                project.layout.projectDirectory
                    .dir("../../common/composeApp/build/XCFrameworks/release/$composeAppFramework")

            val frameworkFolderInSharedModule =
                project.layout.projectDirectory
                    .dir("../../common/iosApp/Frameworks/$composeAppFramework")

            delete(frameworkFolderInSharedModule)
            inputs.dir(frameworkFolderInComposeAppBuildCache)
            outputs.dir(frameworkFolderInSharedModule)

            doFirst {
                println("> Ensuring destination directory exists: $frameworkFolderInSharedModule")
            }
            from(frameworkFolderInComposeAppBuildCache)
            into(frameworkFolderInSharedModule)
            doLast {
                println("> Copied $composeAppFramework to $frameworkFolderInSharedModule")
            }
        }

        val archiveDevice by registering(Exec::class) {
            group = "build"
            description = "xcodebuild archive for iOS Device"
            dependsOn(cleanXcodeBuild, copyComposeAppFrameworkFromBuildToShared)
            inputs.property("scheme", xcodeSchemeName)
            outputs.dir(deviceXCArchiveFolder)

            workingDir = project.projectDir
            commandLine = listOf("xcodebuild", "archive") +
                    commonXcodeFlags +
                    listOf(
                        "-destination", "generic/platform=iOS",
                        "-archivePath", deviceXCArchiveFolder.get().absolutePath
                    )

            doFirst { println("> Archiving DEVICE to ${deviceXCArchiveFolder.get().absolutePath}") }
        }

        val archiveSimulator by registering(Exec::class) {
            group = "build"
            description = "xcodebuild archive for iOS Simulator"
            dependsOn(cleanXcodeBuild,  copyComposeAppFrameworkFromBuildToShared)
            inputs.property("scheme", xcodeSchemeName)
            outputs.dir(simulatorXCArchiveFolder)

            workingDir = project.projectDir
            commandLine = listOf("xcodebuild", "archive") +
                    commonXcodeFlags +
                    listOf(
                        "-destination", "generic/platform=iOS Simulator",
                        "-archivePath", simulatorXCArchiveFolder.get().absolutePath
                    )

            doFirst { println("> Archiving SIMULATOR to ${simulatorXCArchiveFolder.get().absolutePath}") }
        }

        val createXCFramework by registering(Exec::class) {
            group = "build"
            description = "Creates the XCFramework from device & simulator archives"
            dependsOn(archiveDevice, archiveSimulator, cleanXCFramework)
            inputs.files(deviceXCArchiveFolder, simulatorXCArchiveFolder)
            outputs.dir(combinedArchiveFolder)

            workingDir = project.projectDir
            commandLine = listOf("xcodebuild", "-create-xcframework") +
                    listOf(
                        "-archive",
                        deviceXCArchiveFolder.get().absolutePath,
                        "-framework",
                        "${entryFrameworkName}.framework",
                        "-archive",
                        simulatorXCArchiveFolder.get().absolutePath,
                        "-framework",
                        "${entryFrameworkName}.framework",
                        "-output",
                        combinedArchiveFolder.get().absolutePath
                    )

            doFirst { println("> Creating XCFramework at ${combinedArchiveFolder.get().absolutePath}") }
        }

        val copyEntryXCFramework by registering(Copy::class) {
            group = "distribution"
            description = "Copy ${entryFrameworkName}.xcframework into final release directory"
            dependsOn(createXCFramework, copyComposeAppFrameworkFromBuildToShared)
            val frameworkFolderInReleaseModule =
                project.layout.projectDirectory.dir("../ios/Frameworks/${entryFrameworkName}.xcframework")

            delete(frameworkFolderInReleaseModule)

            from(combinedArchiveFolder)
            into(frameworkFolderInReleaseModule)

            inputs.dir(combinedArchiveFolder)
            outputs.dir(frameworkFolderInReleaseModule)

            doFirst { println("> Preparing release dir: $frameworkFolderInReleaseModule") }
            doLast { println("> Released XCFramework to $frameworkFolderInReleaseModule") }
        }

        register("buildAndPackageReleaseIOSFrameworks") {
            group = "build"
            description = "Builds & packages all release iOS frameworks"
            dependsOn(copyEntryXCFramework)
            doLast { println("> ✅ All iOS frameworks built & packaged.") }
        }
    } else {
        println("⚠\uFE0F Can't configure iOS release build tasks because current os is ${current().name} ⚠\uFE0F")
    }
}
