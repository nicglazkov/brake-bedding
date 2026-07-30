package com.glazkov.brakebedding.service

/**
 * The platform work around a run.
 *
 * On Android, a foreground service keeps the process alive and shows a notification.
 * On iOS, background location keeps the app alive, and a Live Activity shows the
 * instruction. The controller only reports the start and the end of a run; each
 * platform supplies the actual behavior.
 */
expect object RunSessionSupport {
    fun onRunStarted()

    fun onRunEnded()
}
