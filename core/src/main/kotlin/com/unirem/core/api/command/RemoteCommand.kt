package com.unirem.core.api.command

/**
 * The five Story 1.11 remote commands. Directions repeat (Story 1.13 owns
 * the engine); Enter never repeats. The wire code lives in `:tv`
 * (`KeyFrameMapper`); `:app` never sees a `KEY_*` constant.
 */
enum class RemoteCommand {
    Up,
    Down,
    Left,
    Right,
    Enter,
}
