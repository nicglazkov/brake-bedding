import ActivityKit
import Foundation
import Shared

/// Owns the run Live Activity. The shared Kotlin code reports the run state through
/// LiveActivityBridge; this class converts the reports into ActivityKit calls.
final class LiveActivityManager {

    static let shared = LiveActivityManager()

    private var activity: Activity<RunActivityAttributes>?

    /// Connects the Kotlin bridge and the intent handlers. Call one time at start.
    func connect() {
        let controller = Run.shared.controller

        RunIntentHandlers.togglePause = { controller.togglePause() }
        RunIntentHandlers.stop = { controller.stop() }

        LiveActivityBridge.shared.onStart = { [weak self] in
            self?.start(procedureName: controller.state.value.procedure.name)
        }
        LiveActivityBridge.shared.onUpdate = { [weak self] content in
            self?.update(content: content)
        }
        LiveActivityBridge.shared.onEnd = { [weak self] in
            self?.end()
        }
    }

    private func start(procedureName: String) {
        guard ActivityAuthorizationInfo().areActivitiesEnabled else { return }
        guard activity == nil else { return }
        let attributes = RunActivityAttributes(procedureName: procedureName)
        let state = RunActivityAttributes.ContentState(
            title: "Brake bedding",
            text: "The run starts",
            phaseName: "IDLE",
            isPaused: false
        )
        activity = try? Activity.request(
            attributes: attributes,
            content: .init(state: state, staleDate: nil)
        )
    }

    private func update(content: RunActivityContent) {
        guard let activity else { return }
        let state = RunActivityAttributes.ContentState(
            title: content.title,
            text: content.text,
            phaseName: content.phaseName,
            isPaused: content.isPaused
        )
        Task {
            await activity.update(.init(state: state, staleDate: nil))
        }
    }

    private func end() {
        guard let activity else { return }
        let final = RunActivityAttributes.ContentState(
            title: "The run has ended",
            text: "",
            phaseName: "IDLE",
            isPaused: false
        )
        Task {
            await activity.end(.init(state: final, staleDate: nil), dismissalPolicy: .immediate)
        }
        self.activity = nil
    }
}
