package com.unirem

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.unirem.core.api.RedactedLog
import com.unirem.ui.graphitemint.GraphiteMintTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        RedactedLog.debug(TAG, "Shell created")
        setContent {
            GraphiteMintTheme {
                ShellScreen(versionName = BuildConfig.VERSION_NAME)
            }
        }
    }

    companion object {
        private const val TAG = "Shell"
    }
}
