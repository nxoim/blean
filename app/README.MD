# Note on the build process for iOS

`common` contains shared code for iOS, and its build folders are used for packaging the xcframeworks. 

`debug` builds normally, depending on `common` (via `Package.swift`)

`release` build process is custom. `common` is assembled into an XCFramework that includes compiled kotlin/native sources(`ComposeApp`) which is then copied over to into the release XCode project. 

To compile `release` for iOS - run `./gradlew release:compose:buildAndPackageIOSFrameworks` first (will compile the framework and copy it over) and then build normally.
⚠️Please note that for running the iOS release build you really should add the above gradle task before "Run iOS application" in the run configuration settings⚠️

Note about the way the iOS framework is compiled:
The `ComposeApp.xcframework` is BUNDLED with `ComposeCommonAppEntry.xcframework` ANYWAYS. The release xcode project is configured to find the bundled `ComposeApp.xcframework`.