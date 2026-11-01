package io.github.corneloaie.admob

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import platform.Foundation.NSDate
import platform.Foundation.NSRunLoop
import platform.Foundation.NSThread
import platform.Foundation.dateWithTimeIntervalSinceNow
import platform.Foundation.runUntilDate
import platform.Foundation.timeIntervalSinceNow
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Runs [block] on the main dispatcher and keeps the main run loop turning until it ends. The SDKs call back on the main
 * queue, which runBlocking and runTest would block.
 */
internal fun <T> runOnMainLoop(timeoutSeconds: Double = 60.0, block: suspend CoroutineScope.() -> T): T {
    assertTrue(NSThread.isMainThread, "Kotlin/Native runs tests on the main thread")
    var outcome: Result<T>? = null
    val job = CoroutineScope(Dispatchers.Main).launch { outcome = runCatching { block() } }
    val deadline = NSDate.dateWithTimeIntervalSinceNow(timeoutSeconds)
    while (outcome == null && deadline.timeIntervalSinceNow > 0) {
        NSRunLoop.mainRunLoop.runUntilDate(NSDate.dateWithTimeIntervalSinceNow(0.05))
    }
    job.cancel()
    return (outcome ?: fail("Timed out after $timeoutSeconds s")).getOrThrow()
}
