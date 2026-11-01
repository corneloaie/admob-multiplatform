package io.github.corneloaie.admob

import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import com.google.android.gms.ads.rewarded.RewardedAd as GmaRewardedAd
import com.google.android.gms.ads.rewarded.ServerSideVerificationOptions as GmaServerSideVerificationOptions

public actual class RewardedAd internal constructor(
    /** The Google Mobile Ads SDK's ad. */
    public val android: GmaRewardedAd,
    private val session: FullScreenAdSession,
) {
    public actual val adUnitId: String get() = android.adUnitId
    public actual val responseInfo: ResponseInfo? get() = android.responseInfo.toCommon()
    public actual val rewardItem: RewardItem get() = android.rewardItem.let { RewardItem(it.type, it.amount) }
    public actual val events: Flow<AdEvent> get() = session.events.flow

    public actual fun setServerSideVerificationOptions(options: ServerSideVerificationOptions) {
        android.setServerSideVerificationOptions(
            GmaServerSideVerificationOptions.Builder()
                .apply {
                    options.userId?.let { setUserId(it) }
                    options.customData?.let { setCustomData(it) }
                }
                .build(),
        )
    }

    public actual suspend fun show(host: AdHost): RewardItem? = withContext(Dispatchers.Main.immediate) {
        session.show {
            android.show(host) { item -> session.onRewardEarned(RewardItem(item.type, item.amount)) }
        }
    }

    public actual companion object {
        public actual suspend fun load(adUnitId: String, request: AdRequest): RewardedAd =
            withContext(Dispatchers.Main.immediate) {
                suspendCancellableCoroutine { continuation ->
                    GmaRewardedAd.load(
                        applicationContext(),
                        adUnitId,
                        request.toAndroid(),
                        object : RewardedAdLoadCallback() {
                            override fun onAdLoaded(ad: GmaRewardedAd) {
                                val session = FullScreenAdSession()
                                ad.fullScreenContentCallback = session.contentCallback()
                                ad.onPaidEventListener = session.paidEventListener()
                                continuation.resume(RewardedAd(ad, session))
                            }

                            override fun onAdFailedToLoad(error: LoadAdError) {
                                continuation.resumeWithException(AdMobException(error.toAdError()))
                            }
                        },
                    )
                }
            }
    }
}
