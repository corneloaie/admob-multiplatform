package io.github.corneloaie.admob

import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * The common part of a full-screen ad: the platform code routes the SDK's full-screen callbacks to the `on` functions,
 * which feed [events] and end a running [show]. Everything runs on the main thread.
 */
internal class FullScreenAdSession {
    val events = AdEvents()
    private var showing: CancellableContinuation<RewardItem?>? = null
    private var reward: RewardItem? = null

    /** Calls [present] and returns when the ad is dismissed: the reward, if one was earned. */
    suspend fun show(present: () -> Unit): RewardItem? {
        check(showing == null) { "This ad is already showing" }
        return suspendCancellableCoroutine { continuation ->
            showing = continuation
            continuation.invokeOnCancellation { showing = null }
            present()
        }
    }

    fun onShown() = events.emit(AdEvent.Shown)

    fun onImpression() = events.emit(AdEvent.Impression)

    fun onClick() = events.emit(AdEvent.Click)

    fun onPaid(value: AdValue) = events.emit(AdEvent.Paid(value))

    fun onRewardEarned(item: RewardItem) {
        reward = item
        events.emit(AdEvent.RewardEarned(item))
    }

    fun onDismissed() {
        events.emit(AdEvent.Dismissed)
        events.close()
        finish()?.resume(reward)
    }

    fun onFailedToShow(error: AdError) {
        events.emit(AdEvent.FailedToShow(error))
        events.close()
        finish()?.resumeWithException(AdMobException(error))
    }

    // Dropping the continuation also drops the shown ad's reference to the caller's coroutine.
    private fun finish() = showing.also { showing = null }
}
