package io.github.corneloaie.admob

import android.os.Handler
import android.os.Looper

private val mainHandler by lazy { Handler(Looper.getMainLooper()) }

/**
 * Runs [block] on the main thread, after what is already queued there. The GMA Next-Gen SDK calls back on background
 * threads; the library's state is kept on the main one, and queueing every callback keeps them in the order they came.
 */
internal fun postToMainThread(block: () -> Unit) {
    mainHandler.post(block)
}
