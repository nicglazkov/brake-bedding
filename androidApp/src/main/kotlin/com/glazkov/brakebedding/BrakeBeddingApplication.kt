package com.glazkov.brakebedding

import android.app.Application
import android.content.Intent
import android.os.Build
import com.glazkov.brakebedding.platform.PlatformContext
import com.glazkov.brakebedding.service.RunService
import com.glazkov.brakebedding.service.RunSessionSupport

/**
 * Initializes the shared library: the application context, and the lambda that
 * starts the foreground service when a run starts.
 */
class BrakeBeddingApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        PlatformContext.init(this)
        RunSessionSupport.startService = {
            val intent = Intent(this, RunService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        }
    }
}
