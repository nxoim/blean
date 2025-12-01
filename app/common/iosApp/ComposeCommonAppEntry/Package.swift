// swift-tools-version: 5.10
import PackageDescription

let package = Package(
    name: "ComposeCommonAppEntry",
    platforms: [.iOS(.v15)],
    products: [
        .library(
            name: "ComposeCommonAppEntry",
            targets: ["ComposeCommonAppEntry"]
        )
    ],
    targets: [
        .target(name: "ComposeCommonAppEntry")
    ]
)
