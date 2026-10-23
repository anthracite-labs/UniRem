package com.unirem.tv.internal

import com.unirem.core.api.command.RemoteCommand

/**
 * One tap maps to exactly one `ms.remote.control` Click frame (Story 1.11).
 * Directions carry `KEY_*`; OK is Enter. There are no `Press`/`Release`
 * frames here: Story 1.13 owns holds. Internal to `:tv`; `:app` never sees
 * a `KEY_*` code or a frame shape.
 */
internal object KeyFrameWriter {

    const val METHOD = "ms.remote.control"
    const val CMD_CLICK = "Click"
    const val TYPE_SEND_KEY = "SendRemoteKey"

    fun dataOfCmd(command: RemoteCommand): String = when (command) {
        RemoteCommand.Up -> "KEY_UP"
        RemoteCommand.Down -> "KEY_DOWN"
        RemoteCommand.Left -> "KEY_LEFT"
        RemoteCommand.Right -> "KEY_RIGHT"
        RemoteCommand.Enter -> "KEY_ENTER"
    }

    /** Single Click frame; exactly one `DataOfCmd`, never a queue. */
    fun frameFor(command: RemoteCommand): String =
        "{\"method\":\"$METHOD\",\"params\":" +
            "{\"Cmd\":\"$CMD_CLICK\"," +
            "\"DataOfCmd\":\"${dataOfCmd(command)}\"," +
            "\"Option\":\"false\"," +
            "\"TypeOfRemote\":\"$TYPE_SEND_KEY\"}}"
}
