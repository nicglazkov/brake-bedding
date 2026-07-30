import ActivityKit
import Foundation

/// The data of the run Live Activity. The app target and the widget target both
/// compile this file, because the two sides must agree on the exact shape.
struct RunActivityAttributes: ActivityAttributes {
    struct ContentState: Codable, Hashable {
        /// The applicable instruction, from the common RunTexts code.
        var title: String
        /// The stage and stop position.
        var text: String
        /// The name of the run phase. The widget maps it to the phase color.
        var phaseName: String
        var isPaused: Bool
    }

    /// The procedure name at the start of the run.
    var procedureName: String
}
