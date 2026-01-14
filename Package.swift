// swift-tools-version:5.9
import PackageDescription

let package = Package(
    name: "ExpoMapboxNavigation",
    platforms: [.iOS(.v13)],
    products: [
        .library(name: "ExpoMapboxNavigation", targets: ["ExpoMapboxNavigation"])
    ],
    dependencies: [
        .package(url: "https://github.com/mapbox/mapbox-navigation-ios.git", from: "3.12.0")
    ],
    targets: [
        .target(
            name: "ExpoMapboxNavigation",
            dependencies: [
                .product(name: "MapboxNavigationCore", package: "mapbox-navigation-ios"),
                .product(name: "MapboxNavigationUIKit", package: "mapbox-navigation-ios"),
            ],
            path: "ios"
        )
    ]
)
