package com.glazkov.brakebedding.service

/**
 * The Android run session: a foreground service.
 *
 * The service class is in the app module, not in this library. Because of this, the
 * app registers the two lambdas at start time, and this actual only calls them.
 */
actual object RunSessionSupport {

    var startService: (() -> Unit)? = null
    var stopService: (() -> Unit)? = null

    actual fun onRunStarted() {
        startService?.invoke()
    }

    actual fun onRunEnded() {
        // The Android service watches the controller state and stops itself. The stop
        // lambda stays for completeness and for tests.
        stopService?.invoke()
    }
}
