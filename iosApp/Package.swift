// swift-tools-version: 6.0
// Paquete auxiliar para compilar/verificar el coordinador nativo en iOS.
// La aplicación instalable se genera desde project.yml con XcodeGen.
import PackageDescription

let package = Package(
    name: "GamoPanelPasskeys",
    platforms: [.iOS(.v17)],
    products: [.library(name: "GamoPanelPasskeys", targets: ["GamoPanelPasskeys"])],
    targets: [
        .target(name: "GamoPanelPasskeys", path: "Sources/GamoPanel", exclude: ["GamoPanelApp.swift"])
    ]
)
