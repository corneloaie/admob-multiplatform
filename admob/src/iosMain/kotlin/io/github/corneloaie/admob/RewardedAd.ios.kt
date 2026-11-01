package io.github.corneloaie.admob

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import swiftPMImport.io.github.corneloaie.admob.GADRewardedAd
import swiftPMImport.io.github.corneloaie.admob.GADServerSideVerificationOptions
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

public actual class RewardedAd internal constructor(
    /** The Google Mobile Ads SDK's ad. */
    public val ios: GADRewardedAd,
    private val session: FullScreenAdSession,
) {
    public actual val adUnitId: String get() = ios.adUnitID
    public actual val responseInfo: ResponseInfo? get() = ios.responseInfo.toCommon()
    public actual val rewardItem: RewardItem get() = ios.adReward.toCommon()
    public actual val events: Flow<AdEvent> get() = session.events.flow

    public actual fun setServerSideVerificationOptions(options: ServerSideVerificationOptions) {
        ios.serverSideVerificationOptions = GADServerSideVerificationOptions().also {
            it.userIdentifier = options.userId
            it.customRewardString = options.customData
        }
    }

    public actual suspend fun show(host: AdHost): RewardItem? = withContext(Dispatchers.Main.immediate) {
        // Read before presenting: a handler that captured the ad, which holds the handler, would be a retain cycle.
        val reward = rewardItem
        session.show {
            ios.presentFromRootViewController(host, userDidEarnRewardHandler = { session.onRewardEarned(reward) })
        }
    }

    public actual companion object {
        public actual suspend fun load(adUnitId: String, request: AdRequest): RewardedAd =
            withContext(Dispatchers.Main.immediate) {
                suspendCancellableCoroutine { continuation ->
                    GADRewardedAd.loadWithAdUnitID(adUnitId, request.toIos()) { ad, error ->
                        if (ad != null) {
                            val session = FullScreenAdSession()
                            ad.attach(session)
                            ad.paidEventHandler = session.paidEventHandler()
                            continuation.resume(RewardedAd(ad, session))
                        } else {
                            continuation.resumeWithException(AdMobException(error.toLoadError()))
                        }
                    }
                }
            }
    }
}
