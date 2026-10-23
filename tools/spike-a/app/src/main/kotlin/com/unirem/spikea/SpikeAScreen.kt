package com.unirem.spikea

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/**
 * One scrollable run log and the run buttons. This is a harness surface, not a
 * product UI: no remote, no switcher, no settings. Text is never translated,
 * so lint's SetTextI18n is suppressed for the whole file.
 */
@SuppressLint("SetTextI18n")
class SpikeAScreen(
    private val activity: Activity,
    private val harness: Harness,
) {

    private val root = LinearLayout(activity).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(12), dp(12), dp(12), dp(12))
    }

    private val status = TextView(activity).apply {
        textSize = 12f
        setTextIsSelectable(true)
        setPadding(0, dp(6), 0, dp(6))
    }

    private val backoff = TextView(activity).apply {
        textSize = 12f
        setTextIsSelectable(true)
        setPadding(0, 0, 0, dp(6))
    }

    private val logView = TextView(activity).apply {
        typeface = Typeface.MONOSPACE
        textSize = 11f
        setTextIsSelectable(true)
        setPadding(dp(8), dp(8), dp(8), dp(8))
    }

    private val logScroll = ScrollView(activity).apply {
        addView(logView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        setPadding(0, dp(4), 0, 0)
    }

    private val ipInput = field("TV IPv4 (manual)")
    private val labelInput = field("TV label (optional)")
    private val regionInput = field("Region (e.g. EU)")
    private val yearInput = field("Model year (e.g. 2024)", InputType.TYPE_CLASS_NUMBER)

    private val allowWithoutRemote = CheckBox(activity).apply {
        text = "Allow answered without the TV remote"
        textSize = 12f
    }

    private val rePromptSeen = CheckBox(activity).apply {
        text = "TV showed the Allow prompt on reconnect"
        textSize = 12f
    }

    private val buttons = mutableListOf<Button>()
    private val keyButtons = mutableListOf<Button>()

    private val ticker = Handler(Looper.getMainLooper())
    private val tick = object : Runnable {
        override fun run() {
            val seconds = harness.reconnectInSeconds()
            backoff.text = if (seconds > 0) {
                "Backoff: reconnect allowed in ${seconds}s (no automatic retry)"
            } else {
                "Reconnect: ready (manual only)"
            }
            ticker.postDelayed(this, 1_000L)
        }
    }

    private val outer = ScrollView(activity).apply {
        addView(root, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    init {
        build()
    }

    fun view(): View = outer

    fun start() {
        ticker.removeCallbacks(tick)
        ticker.post(tick)
    }

    fun stop() {
        ticker.removeCallbacks(tick)
    }

    fun appendLog(line: String) {
        logView.append(line + "\n")
        logScroll.post { logScroll.fullScroll(View.FOCUS_DOWN) }
    }

    fun render(state: Harness.State) {
        val text = buildString {
            append("Active: ").append(state.activeTarget.ifBlank { "none" })
            append("\nTvId: ").append(state.activeTvId.ifBlank { "-" })
            if (state.sessionOnly) append(" (session-only)")
            append("\nStored pairing: ").append(if (state.paired) "yes" else "no")
            append("\nDiscovered targets: ").append(state.targetCount)
            append("\nResults: ").append(state.resultsPath)
            append(" (").append(state.resultLines).append(" rows)")
            if (state.rePairRequired) {
                append("\nPairing is required for this TV.")
            }
            append("\nKey session: ").append(if (state.keyConnected) "open" else "closed")
            append(" | Keys: ").append(if (state.keysEnabled) "enabled" else "disabled")
            if (state.lastKey.isNotBlank()) {
                append("\nLast key: ").append(state.lastKey)
                append(" | Evidence: ").append(state.lastEvidence.ifBlank { "unconfirmed" })
                if (state.lastLatencyMs >= 0) {
                    append(" | Latency: ").append(state.lastLatencyMs).append("ms")
                }
            }
        }
        status.text = text
        buttons.forEach { it.isEnabled = !state.running }
        // Key-affecting buttons only make sense on an open session; without the
        // gate every tap while closed would spam blocked rows. Open keys, scan,
        // and read stay available while closed.
        keyButtons.forEach { it.isEnabled = !state.running && state.keysEnabled }
    }

    private fun build() {
        root.addView(
            TextView(activity).apply {
                text = "Spike A: pairing & identity"
                textSize = 20f
                setPadding(0, 0, 0, dp(6))
            },
        )
        root.addView(
            TextView(activity).apply {
                text = "Throwaway harness. Rows are evidence; a green build is not a pass. " +
                    "Results are append-only and redacted at write."
                textSize = 12f
                setPadding(0, 0, 0, dp(8))
            },
        )
        root.addView(status)
        root.addView(backoff)

        twoFields(ipInput, labelInput)
        twoFields(regionInput, yearInput)
        root.addView(allowWithoutRemote)
        root.addView(rePromptSeen)

        addButtonRow("Scan SSDP" to { harness.scan(year(), region(), label()) })
        addButtonRow(
            "Read /api/v2/" to { harness.readManual(ip(), year(), region(), label()) },
            "Pair (Allow)" to { harness.pair(allowWithoutRemote.isChecked) },
        )
        addButtonRow(
            "Deny test" to { harness.deny() },
            "Reconnect" to { harness.reconnect(rePromptSeen.isChecked) },
        )
        addButtonRow(
            "Revalidate pin" to { harness.revalidatePin() },
            "Two-TV check" to { harness.twoTvCheck() },
        )
        addButtonRow(
            "Synthetic collision" to { harness.syntheticCollision() },
            "Next discovered TV" to { harness.cycleTarget() },
        )
        addButtonRow(
            "Clear pairing" to { harness.clearPairing() },
            "Inspect security" to { harness.securityInspect() },
        )
        root.addView(
            TextView(activity).apply {
                text = "Keys (Story 1.2): open the session, then one press per key."
                textSize = 12f
                setPadding(0, dp(8), 0, 0)
            },
        )
        addButtonRow(
            "Open keys" to { harness.openKeys() },
            "Close keys" to { harness.closeKeys() },
        )
        addButtonRow(
            "Up" to { harness.sendKey("UP") },
            "Down" to { harness.sendKey("DOWN") },
            needsKeys = true,
        )
        addButtonRow(
            "Left" to { harness.sendKey("LEFT") },
            "Right" to { harness.sendKey("RIGHT") },
            needsKeys = true,
        )
        addButtonRow(
            "OK" to { harness.sendKey("OK") },
            "Back" to { harness.sendKey("BACK") },
            needsKeys = true,
        )
        addButtonRow(
            "Home" to { harness.sendKey("HOME") },
            "Apps" to { harness.sendKey("APPS") },
            needsKeys = true,
        )
        addButtonRow(
            "Search" to { harness.sendKey("SEARCH") },
            "Power off" to { harness.powerOff() },
            needsKeys = true,
        )
        addButtonRow(
            "Vol+" to { harness.sendKey("VOL_UP") },
            "Vol-" to { harness.sendKey("VOL_DOWN") },
            needsKeys = true,
        )
        addButtonRow(
            "Mute" to { harness.sendKey("MUTE") },
            "Probe power" to { harness.probePower() },
            needsKeys = true,
        )
        addButtonRow(
            "Play" to { harness.sendKey("PLAY") },
            "Pause" to { harness.sendKey("PAUSE") },
            needsKeys = true,
        )
        addButtonRow(
            "Hold Up" to { harness.holdStart("UP") },
            "Hold Down" to { harness.holdStart("DOWN") },
            needsKeys = true,
        )
        addButtonRow(
            "Hold Left" to { harness.holdStart("LEFT") },
            "Hold Right" to { harness.holdStart("RIGHT") },
            needsKeys = true,
        )
        addButtonRow(
            "Release hold" to { harness.holdStop() },
            needsKeys = true,
        )
        addButtonRow(
            "Observed" to { harness.markObserved() },
            "Not observed" to { harness.markNotObserved() },
            needsKeys = true,
        )
        addButtonRow(
            "Observed: machine" to { harness.markMachineObserved() },
            "Observed: probe" to { harness.markProbeObserved() },
            needsKeys = true,
        )
        addButtonRow(
            "Mark unsupported" to { harness.markLastUnsupported() },
            "Latency summary" to { harness.latencySummary() },
            needsKeys = true,
        )

        root.addView(
            TextView(activity).apply {
                text = "Run log"
                textSize = 12f
                setPadding(0, dp(8), 0, 0)
            },
        )
        root.addView(
            logScroll,
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(240)),
        )
    }

    private fun twoFields(left: EditText, right: EditText) {
        val row = LinearLayout(activity).apply { orientation = LinearLayout.HORIZONTAL }
        row.addView(
            left,
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                setMargins(0, dp(2), dp(4), dp(2))
            },
        )
        row.addView(
            right,
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                setMargins(dp(4), dp(2), 0, dp(2))
            },
        )
        root.addView(row)
    }

    private fun addButtonRow(vararg entries: Pair<String, () -> Unit>, needsKeys: Boolean = false) {
        val row = LinearLayout(activity).apply { orientation = LinearLayout.HORIZONTAL }
        entries.forEach { (label, action) ->
            val button = Button(activity).apply {
                text = label
                textSize = 12f
                isAllCaps = false
                setOnClickListener { action() }
            }
            buttons += button
            if (needsKeys) keyButtons += button
            row.addView(
                button,
                LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    setMargins(dp(2), dp(2), dp(2), dp(2))
                },
            )
        }
        if (entries.size == 1) {
            row.addView(View(activity), LinearLayout.LayoutParams(0, 1, 1f))
        }
        root.addView(row)
    }

    private fun field(hint: String, inputType: Int = InputType.TYPE_CLASS_TEXT): EditText =
        EditText(activity).apply {
            this.hint = hint
            this.inputType = inputType
            setSingleLine(true)
            textSize = 14f
        }

    private fun ip(): String = ipInput.text?.toString()?.trim().orEmpty()

    private fun label(): String = labelInput.text?.toString()?.trim().orEmpty()

    private fun region(): String = regionInput.text?.toString()?.trim().orEmpty()

    private fun year(): String = yearInput.text?.toString()?.trim().orEmpty()

    private fun dp(value: Int): Int = (value * activity.resources.displayMetrics.density).toInt()
}
