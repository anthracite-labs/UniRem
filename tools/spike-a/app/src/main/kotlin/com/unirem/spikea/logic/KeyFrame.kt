package com.unirem.spikea.logic

import org.json.JSONObject

/**
 * The MVP key table plus Click/Press/Release frame builders for
 * `ms.remote.control` on `samsung.remote.control`.
 *
 * One gesture maps to exactly one frame: a tap is one `Click`, a hold starts
 * with one `Press` and ends with one `Release`. Repeat ticks reuse `Click`
 * frames under the same gesture generation and never submit on their own; a
 * socket write is never recorded as a TV action. Frame shape follows
 * `protocol-reference.md` and the field-tested `SendRemoteKey` layout
 * (`Cmd` / `DataOfCmd` / `Option=false` / `TypeOfRemote=SendRemoteKey`).
 */
enum class MvpKey(val label: String, val dataOfCmd: String, val group: String) {
    DPAD_UP("Up", "KEY_UP", "nav"),
    DPAD_DOWN("Down", "KEY_DOWN", "nav"),
    DPAD_LEFT("Left", "KEY_LEFT", "nav"),
    DPAD_RIGHT("Right", "KEY_RIGHT", "nav"),
    OK("OK", "KEY_ENTER", "nav"),
    BACK("Back", "KEY_RETURN", "system"),
    HOME("Home", "KEY_HOME", "system"),
    // Provisional wire codes: the protocol short list names neither. KEY_CONTENTS
    // is the long-standing Samsung Smart Hub / Apps-panel key and KEY_SEARCH
    // follows the KEY_* convention for the documented Search key. A firmware
    // that ignores either is recorded unsupported (one-time chip candidate),
    // never failed; the hardware run settles the code.
    APPS("Apps", "KEY_CONTENTS", "system"),
    SEARCH("Search", "KEY_SEARCH", "system"),
    VOL_UP("Vol+", "KEY_VOLUP", "volume"),
    VOL_DOWN("Vol-", "KEY_VOLDOWN", "volume"),
    MUTE("Mute", "KEY_MUTE", "volume"),
    PLAY("Play", "KEY_PLAY", "transport"),
    PAUSE("Pause", "KEY_PAUSE", "transport"),
    // Distinct discrete power key. There is no toggle behavior in the harness:
    // one Click is sent and the TV state is probed afterwards. The protocol
    // reference settles this as KEY_POWER.
    POWER("Power", "KEY_POWER", "power"),
    ;

    companion object {
        /** Screen/harness names: UP, DOWN, LEFT, RIGHT, OK, BACK, HOME, APPS, ... */
        fun byName(name: String): MvpKey? {
            val wanted = name.trim().uppercase()
            return entries.firstOrNull {
                it.name == wanted ||
                    it.name.removePrefix("DPAD_") == wanted ||
                    it.name.removePrefix("VOL_") == wanted
            }
        }

        fun fromDataOfCmd(dataOfCmd: String): MvpKey? =
            entries.firstOrNull { it.dataOfCmd == dataOfCmd }
    }
}

object KeyFrame {

    const val METHOD = "ms.remote.control"
    const val TYPE_SEND_KEY = "SendRemoteKey"
    const val OPTION = "false"

    const val CMD_CLICK = "Click"
    const val CMD_PRESS = "Press"
    const val CMD_RELEASE = "Release"

    /** D-pad directions are the only keys the single repeat engine may repeat. */
    val REPEATABLE: Set<MvpKey> = setOf(
        MvpKey.DPAD_UP,
        MvpKey.DPAD_DOWN,
        MvpKey.DPAD_LEFT,
        MvpKey.DPAD_RIGHT,
    )

    fun isRepeatable(key: MvpKey): Boolean = key in REPEATABLE

    fun click(key: MvpKey): String = frame(CMD_CLICK, key.dataOfCmd)

    fun press(key: MvpKey): String = frame(CMD_PRESS, key.dataOfCmd)

    fun release(key: MvpKey): String = frame(CMD_RELEASE, key.dataOfCmd)

    fun frame(cmd: String, dataOfCmd: String): String {
        val params = JSONObject()
            .put("Cmd", cmd)
            .put("DataOfCmd", dataOfCmd)
            .put("Option", OPTION)
            .put("TypeOfRemote", TYPE_SEND_KEY)
        return JSONObject().put("method", METHOD).put("params", params).toString()
    }

    data class ParsedFrame(val cmd: String, val dataOfCmd: String)

    /** Parses back a frame this object built; null when the shape is foreign. */
    fun parse(text: String): ParsedFrame? = try {
        val json = JSONObject(text)
        if (json.optString("method") != METHOD) {
            null
        } else {
            val params = json.optJSONObject("params") ?: return null
            if (params.optString("TypeOfRemote") != TYPE_SEND_KEY) {
                null
            } else {
                ParsedFrame(params.optString("Cmd"), params.optString("DataOfCmd"))
            }
        }
    } catch (e: Exception) {
        null
    }
}
