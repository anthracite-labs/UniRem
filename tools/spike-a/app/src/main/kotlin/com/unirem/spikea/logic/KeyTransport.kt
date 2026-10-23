package com.unirem.spikea.logic

/**
 * Narrow seam around the key socket so orchestration is JVM-testable. The
 * real path is `KeySession` (OkHttp); tests use a fake. Either way a socket
 * write is never a TV action: `send` reports only socket acceptance.
 */
interface KeyTransport {
    fun isOpen(): Boolean
    fun send(frameJson: String): Boolean
    fun close(reason: String)
}
