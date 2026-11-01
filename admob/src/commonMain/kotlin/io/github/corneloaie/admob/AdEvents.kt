package io.github.corneloaie.admob

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.transformWhile

/**
 * One ad's events. The platform code wires the SDK's listeners to [emit] at load time, before the app has the ad: the
 * SDK delivers the paid event only to a listener in place at impression time. Every collector of [flow] gets every
 * event from the start, however late it starts collecting, and the flow completes once [close] is called.
 */
internal class AdEvents {
    // null marks the end. An ad reports a handful of events, well under what is replayed.
    private val events = MutableSharedFlow<AdEvent?>(replay = REPLAYED_EVENTS, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    private var closed = false

    val flow: Flow<AdEvent> = events.transformWhile { event ->
        if (event != null) emit(event)
        event != null
    }

    fun emit(event: AdEvent) {
        if (!closed) events.tryEmit(event)
    }

    fun close() {
        if (closed) return
        closed = true
        events.tryEmit(null)
    }
}

private const val REPLAYED_EVENTS = 64
