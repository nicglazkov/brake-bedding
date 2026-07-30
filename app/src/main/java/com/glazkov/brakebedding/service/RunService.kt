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
 * Keeps a run alive when the app is not on the screen.
 *
 * The run is in [RunController]. This service makes sure that Android keeps the
 * process and the GPS data active when the driver gets a call or the screen goes
 * off. It also shows the applicable instruction in the notification. The service
 * monitors the controller and stops itself when the run ends. It owns no data.
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

        // startForeground must occur quickly after startForegroundService. This is
        // applicable to each start, and also to the starts that supply an action.
        startAsForeground()
        observeRun()
        return START_NOT_STICKY
    }

    /**
     * If the user removes the task, the run ends. This is the result that the user
     * expects. A voice with brake instructions from an app that is not visible is
     * not safe.
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
                // The notification changes only when its content changes. A change at
                // each 250 ms tick would be too frequent. The distance is in steps of
                // 200 m. Because of this, the distance in the title moves through a
                // gap or a cooldown, but the notification changes only after some
                // seconds. Without the distance in the key, the title kept the
                // initial distance of the phase.
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
                        // FINISHED keeps a last summary that the user can remove.
                        // IDLE shows that the user stopped the run. Then the app
                        // removes the notification.
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
            snapshot.run.isPaused -> "The run is on pause"
            phase == RunPhase.FINISHED -> "The procedure is complete"
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
                if (snapshot.run.isPaused) "Continue" else "Pause",
                serviceAction(ACTION_TOGGLE_PAUSE),
            )
            builder.addAction(0, "Stop", serviceAction(ACTION_STOP))
        }

        return builder.build()
    }

    /** The notification titles obey ASD-STE100. */
    private fun instructionFor(snapshot: RunSnapshot, units: UnitSystem): String {
        val stage = snapshot.currentStage
        return when (snapshot.run.phase) {
            RunPhase.SPEED_UP -> (stage as? BeddingStage)
                ?.let { "Increase speed to ${units.formatSpeedWithUnit(it.startSpeedMps)}" }
                ?: "Increase speed"

            RunPhase.SLOW_DOWN -> (stage as? BeddingStage)
                ?.let { "Decrease speed to ${units.formatSpeedWithUnit(it.startSpeedMps)}" }
                ?: "Decrease speed"

            RunPhase.HOLD -> (stage as? BeddingStage)
                ?.let { "Hold ${units.formatSpeedWithUnit(it.startSpeedMps)}" } ?: "Hold this speed"

            RunPhase.BRAKE -> (stage as? BeddingStage)
                ?.let { "Brake to ${units.formatSpeedWithUnit(it.targetSpeedMps)}" } ?: "Brake"

            RunPhase.GAP ->
                "Drive ${units.formatDistanceWithUnit(snapshot.run.remainingMeters)} more"

            RunPhase.COOLDOWN ->
                "Cooldown: drive ${units.formatDistanceWithUnit(snapshot.run.remainingMeters)} more"

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
            // Low importance. The voice gives the urgent cues. The notification is
            // only for the eyes, and it must not make its own sound.
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Shows the applicable instruction when a run is active"
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
