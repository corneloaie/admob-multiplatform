package io.github.corneloaie.admob

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Runs [start] once, on the first [await] or [start], and hands its result to every caller. The result belongs to no
 * caller's coroutine, so cancelling one doesn't cancel the start for the others. If [start] throws, the call that ran it
 * throws, and the next one tries again.
 */
internal class OnceInitializer<T>(private val start: (onComplete: (T) -> Unit) -> Unit) {
    private val mutex = Mutex()
    private var result: CompletableDeferred<T>? = null

    suspend fun await(): T = start().await()

    /** Starts it unless it has started, and returns the result to come. */
    suspend fun start(): Deferred<T> = mutex.withLock {
        result ?: CompletableDeferred<T>().also { deferred ->
            start { deferred.complete(it) }
            result = deferred
        }
    }
}
