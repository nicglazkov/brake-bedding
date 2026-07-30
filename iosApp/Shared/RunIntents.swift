import AppIntents

/// The buttons of the Live Activity start these intents. A LiveActivityIntent runs
/// in the app process. There, the app target forwards the call to the shared run
/// controller; the widget target only names the intent types.
struct TogglePauseIntent: LiveActivityIntent {
    static var title: LocalizedStringResource = "Pause or continue the run"

    func perform() async throws -> some IntentResult {
        RunIntentHandlers.togglePause?()
        return .result()
    }
}

struct StopRunIntent: LiveActivityIntent {
    static var title: LocalizedStringResource = "Stop the run"

    func perform() async throws -> some IntentResult {
        RunIntentHandlers.stop?()
        return .result()
    }
}

/// The app target sets these at start time. In the widget target they stay nil,
/// because the intents do not run there.
enum RunIntentHandlers {
    nonisolated(unsafe) static var togglePause: (() -> Void)?
    nonisolated(unsafe) static var stop: (() -> Void)?
}
