package com.unirem

import android.app.Application
import com.unirem.core.api.RedactedLog
import com.unirem.gate.LanAccessGate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Holds the single process-wide [LanAccessGate] so rotation and recreation
 * never reset the permission state; the retained DataStore decision covers
 * process restart.
 */
class UniRemApp : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val gate: LanAccessGate by lazy {
        RedactedLog.debug(TAG, "Gate created")
        LanAccessGate.create(this, appScope)
    }

    companion object {
        private const val TAG = "Shell"
    }
}
