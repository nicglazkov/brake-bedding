import SwiftUI
import Shared

/// The entry point of the iOS app. The UI is the common Compose code from the
/// Shared framework. This file only supplies the window and the Live Activity
/// connection.
@main
struct BrakeBeddingApp: App {

    init() {
        LiveActivityManager.shared.connect()
    }

    var body: some Scene {
        WindowGroup {
            ComposeView()
                .ignoresSafeArea(.all)
        }
    }
}

/// Wraps the Compose UIViewController for SwiftUI.
struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
