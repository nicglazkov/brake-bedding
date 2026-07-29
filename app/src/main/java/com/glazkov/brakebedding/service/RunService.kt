package com.glazkov.brakebedding.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.glazkov.brakebedding.MainActivity
import com.glazkov.brakebedding.R
import com.glazkov.brakebedding.data.BeddingStage
import com.glazkov.brakebedding.data.UnitSystem
import com.glazkov.brakebedding.engine.RunPhase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Keeps a run alive while the app is not on screen.
 *
 * The run itself lives in [RunController]; this service exists so the OS keeps the
 * process and its GPS feed running when the driver takes a call or the screen turns
 * off mid-procedure, and so the current instruction is visible in the notification
 * shade. It observes the controller and stops itself the moment the run is over —
 * it owns nothing.
 */
class RunService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var controller: RunController

    override fun onCreate() {
        super.onCreate()
        controller = RunController.get(this)
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_TOGGLE_PAUSE -> controller.togglePause()
            ACTION_STOP -> controller.stop()
        }

        // startForeground must happen promptly after startForegroundService, on every
        // start including the action re-deliveries.
        startAsForeground()
        observeRun()
        return START_NOT_STICKY
    }

    /**
     * The whole point of the app is coaching a moving car, so if the user swipes the
     * task away the least surprising outcome is that the run ends — not that a voice
     * keeps issuing brake instructions from an app that is no longer anywhere visible.
     */
    override fun onTaskRemoved(rootIntent: Intent?) {
        controller.stop()
        stopSelf()
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startAsForeground() {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private var observing = false

    private fun observeRun() {
        if (observing) return
        observing = true
        scope.launch {
            controller.state
                // Re-render only when something the notification shows has changed;
                // updating on every 250ms tick would spam the shade. Distance is
                // bucketed to 200m so the "to go" figure keeps moving through a gap or
                // cooldown — without it in the key, the title froze at the distance the
                // phase started with — while still updating seconds apart, not 4/sec.
                .map {
                    NotificationKey(
                        phase = it.run.phase,
                        stageIndex = it.run.stageIndex,
                        cycleIndex = it.run.cycleIndex,
                        isPaused = it.run.isPaused,
                        remainingBucket = (it.run.remainingMeters / 200.0).toInt(),
                    )
                }
                .distinctUntilChanged()
                .collect { key ->
                    if (key.phase.isRunning) {
                        notificationManager.notify(NOTIFICATION_ID, buildNotification())
                    } else {
                        // FINISHED leaves a final, dismissible summary; IDLE means the
                        // user stopped the run and wants nothing left behind.
                        if (key.phase == RunPhase.FINISHED) {
                            ServiceCompat.stopForeground(this@RunService, ServiceCompat.STOP_FOREGROUND_DETACH)
                            notificationManager.notify(NOTIFICATION_ID, buildNotification())
                        } else {
                            ServiceCompat.stopForeground(this@RunService, ServiceCompat.STOP_FOREGROUND_REMOVE)
                        }
                        stopSelf()
                    }
                }
        }
    }

    private data class NotificationKey(
        val phase: RunPhase,
        val stageIndex: Int,
        val cycleIndex: Int,
        val isPaused: Boolean,
        val remainingBucket: Int,
    )

    // --- Notification -----------------------------------------------------------------

    private fun buildNotification(): Notification {
        val snapshot = controller.state.value
        val units = snapshot.settings.unitSystem
        val phase = snapshot.run.phase

        val title = when {
            snapshot.run.isPaused -> "Paused"
            phase == RunPhase.FINISHED -> "Bedded"
            else -> instructionFor(snapshot, units)
        }
        val text = when (phase) {
            RunPhase.FINISHED ->
                "${snapshot.run.completedStops} stops · " +
                    units.formatDistanceWithUnit(snapshot.run.distanceTraveledMeters)

            else -> {
                val stage = "Stage ${snapshot.run.stageIndex + 1} of ${snapshot.procedure.stages.size}"
                (snapshot.currentStage as? BeddingStage)
                    ?.let { "$stage · stop ${snapshot.run.cycleIndex + 1} of ${it.numberOfStops}" }
                    ?: stage
            }
        }

        val openApp = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE,
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_rotor)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(openApp)
            .setOngoing(phase.isRunning)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_NAVIGATION)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        if (phase.isRunning) {
            builder.addAction(
                0,
                if (snapshot.run.isPaused) "Resume" else "Pause",
                serviceAction(ACTION_TOGGLE_PAUSE),
            )
            builder.addAction(0, "Stop", serviceAction(ACTION_STOP))
        }

        return builder.build()
    }

    private fun instructionFor(snapshot: RunSnapshot, units: UnitSystem): String {
        val stage = snapshot.currentStage
        return when (snapshot.run.phase) {
            RunPhase.SPEED_UP -> (stage as? BeddingStage)
                ?.let { "Speed up to ${units.formatSpeedWithUnit(it.startSpeedMps)}" } ?: "Speed up"

            RunPhase.SLOW_DOWN -> (stage as? BeddingStage)
                ?.let { "Slow to ${units.formatSpeedWithUnit(it.startSpeedMps)}" } ?: "Slow down"

            RunPhase.HOLD -> (stage as? BeddingStage)
                ?.let { "Hold ${units.formatSpeedWithUnit(it.startSpeedMps)}" } ?: "Hold speed"

            RunPhase.BRAKE -> (stage as? BeddingStage)
                ?.let { "Brake to ${units.formatSpeedWithUnit(it.targetSpeedMps)}" } ?: "Brake"

            RunPhase.GAP -> "Coast — ${units.formatDistanceWithUnit(snapshot.run.remainingMeters)} to go"
            RunPhase.COOLDOWN -> "Cool down — ${units.formatDistanceWithUnit(snapshot.run.remainingMeters)} to go"
            else -> "Brake bedding"
        }
    }

    private fun serviceAction(action: String): PendingIntent = PendingIntent.getService(
        this,
        action.hashCode(),
        Intent(this, RunService::class.java).setAction(action),
        PendingIntent.FLAG_IMMUTABLE,
    )

    private val notificationManager: NotificationManager
        get() = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Active run",
            // Low importance: the voice cues carry urgency; the notification is for
            // glancing at the shade, not for making noise of its own.
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Shows the current instruction while a bedding run is active"
            setShowBadge(false)
        }
        notificationManager.createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_ID = "run"
        const val NOTIFICATION_ID = 1
        const val ACTION_TOGGLE_PAUSE = "com.glazkov.brakebedding.action.TOGGLE_PAUSE"
        const val ACTION_STOP = "com.glazkov.brakebedding.action.STOP"
    }
}
