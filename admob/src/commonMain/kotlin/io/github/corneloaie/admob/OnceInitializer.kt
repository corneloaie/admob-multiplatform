package io.github.corneloaie.admob

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Runs [start] once, on the first [await], and hands its result to every caller. The result belongs to no caller's
 * coroutine, so cancelling one doesn't cancel the start for the others.
 */
internal class OnceInitializer<T>(private val start: (onComplete: (T) -> Unit) -> Unit) {
    private val mutex = Mutex()
    private var result: CompletableDeferred<T>? = null

    suspend fun await(): T {
        val deferred = mutex.withLock {
            result ?: CompletableDeferred<T>().also { deferred ->
                start { deferred.complete(it) }
                result = deferred
            }
        }
        return deferred.await()
    }
}
