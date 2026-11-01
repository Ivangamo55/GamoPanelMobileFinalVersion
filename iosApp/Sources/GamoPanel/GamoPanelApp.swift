// ⚠️ NO VERIFICADO: host Swift mínimo para empaquetar con xtool (Windows/WSL).
import SwiftUI
import ComposeApp // XCFramework generado por Kotlin (ver .github/workflows/ios-framework.yml)

// Puente SwiftUI -> UIKit: muestra el UIViewController de Compose.
struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController() // Función Kotlin de iosMain.
    }
    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

// Punto de entrada de la app iOS/iPadOS.
@main
struct GamoPanelApp: App {
    @StateObject private var passkeys = PasskeyCoordinator()

    var body: some Scene {
        WindowGroup {
            // Compose gestiona los safe areas, por eso SwiftUI no debe recortar nada.
            ComposeView().ignoresSafeArea().environmentObject(passkeys) // leerlo fuerza a SwiftUI a crear el coordinador
        }
    }
}
