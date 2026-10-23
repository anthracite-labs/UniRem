package com.unirem.spikea

import android.app.Activity
import android.os.Bundle

class MainActivity : Activity() {

    private lateinit var harness: Harness
    private lateinit var screen: SpikeAScreen

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        harness = Harness(applicationContext)
        screen = SpikeAScreen(this, harness)
        harness.listener = object : Harness.Listener {
            override fun onLog(line: String) {
                runOnUiThread { screen.appendLog(line) }
            }

            override fun onState(state: Harness.State) {
                runOnUiThread { screen.render(state) }
            }
        }
        setContentView(screen.view())
        screen.render(harness.currentState())
        screen.appendLog("Spike A harness. Results are append-only at ${harness.resultsPath()}.")
        screen.appendLog("Rows are evidence. A green build is not a pass; the report stays blocked until recorded runs exist.")
    }

    override fun onResume() {
        super.onResume()
        screen.start()
    }

    override fun onPause() {
        screen.stop()
        super.onPause()
    }

    override fun onDestroy() {
        harness.listener = null
        harness.shutdown()
        super.onDestroy()
    }
}
