package com.unirem

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import com.unirem.core.api.RedactedLog
import com.unirem.gate.LanAccessGate
import com.unirem.ui.graphitemint.GraphiteMintTheme

class MainActivity : ComponentActivity() {

    private lateinit var gate: LanAccessGate

    // The Activity owns the permission launcher; the gate stays Activity-free
    // and only receives the forwarded result.
    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            RedactedLog.debug(TAG, "Permission result received")
            gate.onPermissionResult(granted)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        gate = (application as UniRemApp).gate
        enableEdgeToEdge()
        RedactedLog.debug(TAG, "Shell created")
        setContent {
            var explainerVisible by rememberSaveable { mutableStateOf(false) }
            GraphiteMintTheme {
                val snapshot by gate.snapshot.collectAsState()
                ShellScreen(
                    versionName = BuildConfig.VERSION_NAME,
                    snapshot = snapshot,
                    explainerVisible = explainerVisible,
                    onGrant = { explainerVisible = true },
                    onExplainerContinue = {
                        gate.onExplainerContinue(
                            firePrompt = {
                                permissionLauncher.launch(LanAccessGate.LOCAL_NETWORK_PERMISSION)
                            },
                            onProceed = {},
                        )
                        explainerVisible = false
                    },
                    onExplainerDismiss = { explainerVisible = false },
                    onOpenSettings = {
                        explainerVisible = false
                        openAppSettings()
                    },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (::gate.isInitialized) {
            gate.refresh()
        }
    }

    private fun openAppSettings() {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
            }
            startActivity(intent)
        } catch (e: Exception) {
            RedactedLog.warn(TAG, "Settings intent failed")
        }
    }

    companion object {
        private const val TAG = "Shell"
    }
}
