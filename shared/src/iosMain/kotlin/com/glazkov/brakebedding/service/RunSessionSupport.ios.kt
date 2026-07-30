package com.glazkov.brakebedding.service

import com.glazkov.brakebedding.location.iosSpeedSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * The iOS run session: background location plus a Live Activity.
 *
 * Background location keeps the GPS flow and the process alive when the app is not
 * on the screen. The Live Activity shows the applicable instruction on the Lock
 * Screen and in the Dynamic Island. The Swift side owns ActivityKit; this object
 * only supplies the texts through [LiveActivityBridge].
 */
actual object RunSessionSupport {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var updateJob: Job? = null

    actual fun onRunStarted() {
        iosSpeedSource.setBackgroundUpdates(true)
        LiveActivityBridge.onStart?.invoke()
        observe()
    }

    actual fun onRunEnded() {
        updateJob?.cancel()
        updateJob = null
        iosSpeedSource.setBackgroundUpdates(false)
        LiveActivityBridge.onEnd?.invoke()
    }

    private fun observe() {
        updateJob?.cancel()
        updateJob = scope.launch {
            Run.controller.state
                // The Live Activity changes only when its content changes. The
                // distance is in steps of 200 m, as in the Android notification.
                .map {
                    RunActivityContent(
                        title = RunTexts.title(it),
                        text = RunTexts.text(it),
                        phaseName = it.run.phase.name,
                        isPaused = it.run.isPaused,
                        isRunning = it.run.phase.isRunning,
                        remainingBucket = (it.run.remainingMeters / 200.0).toInt(),
                    )
                }
                .distinctUntilChanged()
                .collect { content ->
                    LiveActivityBridge.onUpdate?.invoke(content)
                }
        }
    }
}

/** The content of one Live Activity update. */
data class RunActivityContent(
    val title: String,
    val text: String,
    val phaseName: String,
    val isPaused: Boolean,
    val isRunning: Boolean,
    val remainingBucket: Int,
)

/**
 * The connection to the Swift side. The Swift app sets the three closures at start
 * time. Swift also calls the run controls directly through [Run.controller].
 */
object LiveActivityBridge {
    var onStart: (() -> Unit)? = null
    var onUpdate: ((RunActivityContent) -> Unit)? = null
    var onEnd: (() -> Unit)? = null
}
