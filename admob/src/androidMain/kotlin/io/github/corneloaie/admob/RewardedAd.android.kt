package io.github.corneloaie.admob

import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resumeWithException
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAd as GmaRewardedAd
import com.google.android.libraries.ads.mobile.sdk.rewarded.ServerSideVerificationOptions as GmaServerSideVerificationOptions

public actual class RewardedAd internal constructor(
    /** The GMA Next-Gen SDK's ad. */
    public val android: GmaRewardedAd,
    private val session: FullScreenAdSession,
) {
    public actual val adUnitId: String get() = android.adUnitId
    public actual val responseInfo: ResponseInfo? get() = android.getResponseInfo().toCommon()
    public actual val rewardItem: RewardItem get() = android.getRewardItem().let { RewardItem(it.type, it.amount) }
    public actual val events: Flow<AdEvent> get() = session.events.flow

    public actual fun setServerSideVerificationOptions(options: ServerSideVerificationOptions) {
        // The Next-Gen SDK takes an empty string for a value not set.
        android.setServerSideVerificationOptions(
            GmaServerSideVerificationOptions(options.userId.orEmpty(), options.customData.orEmpty()),
        )
    }

    public actual suspend fun show(host: AdHost): RewardItem? = withContext(Dispatchers.Main.immediate) {
        session.show {
            android.show(host) { item ->
                postToMainThread { session.onRewardEarned(RewardItem(item.type, item.amount)) }
            }
        }
    }

    public actual companion object {
        public actual suspend fun load(adUnitId: String, request: AdRequest): RewardedAd =
            withContext(Dispatchers.Main.immediate) {
                awaitStartedSdk()
                suspendCancellableCoroutine { continuation ->
                    GmaRewardedAd.load(
                        request.toAndroid(adUnitId),
                        object : AdLoadCallback<GmaRewardedAd> {
                            // On a background thread; the load returns on the main one.
                            override fun onAdLoaded(ad: GmaRewardedAd) {
                                val session = FullScreenAdSession()
                                ad.adEventCallback = FullScreenEventCallback(session)
                                continuation.resume(RewardedAd(ad, session)) { _, loaded, _ -> loaded.android.destroy() }
                            }

                            override fun onAdFailedToLoad(adError: LoadAdError) {
                                continuation.resumeWithException(AdMobException(adError.toAdError()))
                            }
                        },
                    )
                }
            }
    }
}
