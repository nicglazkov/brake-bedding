import ActivityKit
import SwiftUI
import WidgetKit

/// The run Live Activity: the applicable instruction on the Lock Screen and in the
/// Dynamic Island, with Pause and Stop buttons. The colors are the phase colors of
/// the app, so the two surfaces show one identity.
struct RunLiveActivity: Widget {

    var body: some WidgetConfiguration {
        ActivityConfiguration(for: RunActivityAttributes.self) { context in
            LockScreenView(state: context.state)
                .activityBackgroundTint(phaseColor(context.state.phaseName))
                .activitySystemActionForegroundColor(.white)
        } dynamicIsland: { context in
            DynamicIsland {
                DynamicIslandExpandedRegion(.leading) {
                    PhaseDot(phaseName: context.state.phaseName)
                }
                DynamicIslandExpandedRegion(.center) {
                    Text(context.state.title)
                        .font(.headline)
                        .lineLimit(1)
                        .minimumScaleFactor(0.7)
                }
                DynamicIslandExpandedRegion(.bottom) {
                    HStack {
                        Text(context.state.text)
                            .font(.caption)
                            .foregroundStyle(.secondary)
                        Spacer()
                        ControlButtons(isPaused: context.state.isPaused)
                    }
                }
            } compactLeading: {
                PhaseDot(phaseName: context.state.phaseName)
            } compactTrailing: {
                Text(shortTitle(context.state))
                    .font(.caption2.monospaced())
                    .lineLimit(1)
            } minimal: {
                PhaseDot(phaseName: context.state.phaseName)
            }
        }
    }

    private func shortTitle(_ state: RunActivityAttributes.ContentState) -> String {
        state.isPaused ? "PAUSE" : phaseWord(state.phaseName)
    }
}

private struct LockScreenView: View {
    let state: RunActivityAttributes.ContentState

    var body: some View {
        HStack(spacing: 14) {
            VStack(alignment: .leading, spacing: 4) {
                Text(state.title)
                    .font(.headline)
                    .foregroundStyle(.white)
                    .lineLimit(2)
                    .minimumScaleFactor(0.7)
                Text(state.text)
                    .font(.caption)
                    .foregroundStyle(.white.opacity(0.85))
            }
            Spacer()
            ControlButtons(isPaused: state.isPaused)
        }
        .padding(16)
    }
}

private struct ControlButtons: View {
    let isPaused: Bool

    var body: some View {
        HStack(spacing: 10) {
            Button(intent: TogglePauseIntent()) {
                Image(systemName: isPaused ? "play.fill" : "pause.fill")
            }
            .buttonStyle(.bordered)
            .tint(.white)
            Button(intent: StopRunIntent()) {
                Image(systemName: "stop.fill")
            }
            .buttonStyle(.bordered)
            .tint(.white)
        }
    }
}

private struct PhaseDot: View {
    let phaseName: String

    var body: some View {
        Circle()
            .fill(phaseColor(phaseName))
            .frame(width: 14, height: 14)
    }
}

/// The phase colors, identical to PhasePalette in the shared code.
private func phaseColor(_ phaseName: String) -> Color {
    switch phaseName {
    case "SPEED_UP": return Color(red: 0.043, green: 0.541, blue: 0.322)
    case "SLOW_DOWN": return Color(red: 0.910, green: 0.639, blue: 0.090)
    case "HOLD": return Color(red: 0.086, green: 0.408, blue: 0.769)
    case "BRAKE": return Color(red: 0.851, green: 0.176, blue: 0.125)
    case "GAP": return Color(red: 0.173, green: 0.216, blue: 0.259)
    case "COOLDOWN": return Color(red: 0.067, green: 0.369, blue: 0.349)
    case "FINISHED": return Color(red: 0.761, green: 0.255, blue: 0.047)
    default: return Color(red: 0.173, green: 0.216, blue: 0.259)
    }
}

private func phaseWord(_ phaseName: String) -> String {
    switch phaseName {
    case "SPEED_UP": return "SPEED+"
    case "SLOW_DOWN": return "SPEED−"
    case "HOLD": return "HOLD"
    case "BRAKE": return "BRAKE"
    case "GAP": return "DRIVE"
    case "COOLDOWN": return "COOL"
    case "FINISHED": return "DONE"
    default: return ""
    }
}
