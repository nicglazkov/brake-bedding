import SwiftUI
import Shared

/// The entry point of the iOS app. The UI is the common Compose code from the
/// Shared framework. This file only supplies the window and the Live Activity
/// connection.
@main
struct BrakeBeddingApp: App {

    init() {
        LiveActivityManager.shared.connect()

        #if DEBUG
        // A test hook for the simulator drive. The environment variable comes from
        // simctl. Release builds do not contain this code.
        if ProcessInfo.processInfo.environment["BB_AUTOSTART"] == "1" {
            DispatchQueue.main.asyncAfter(deadline: .now() + 3) {
                Run.shared.controller.start()
            }
        }
        #endif
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
