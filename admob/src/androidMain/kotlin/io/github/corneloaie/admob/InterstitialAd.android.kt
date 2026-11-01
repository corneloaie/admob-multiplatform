package io.github.corneloaie.admob

import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import com.google.android.gms.ads.interstitial.InterstitialAd as GmaInterstitialAd

public actual class InterstitialAd internal constructor(
    /** The Google Mobile Ads SDK's ad. */
    public val android: GmaInterstitialAd,
    private val session: FullScreenAdSession,
) {
    public actual val adUnitId: String get() = android.adUnitId
    public actual val responseInfo: ResponseInfo? get() = android.responseInfo.toCommon()
    public actual val events: Flow<AdEvent> get() = session.events.flow

    public actual suspend fun show(host: AdHost) {
        withContext(Dispatchers.Main.immediate) { session.show { android.show(host) } }
    }

    public actual companion object {
        public actual suspend fun load(adUnitId: String, request: AdRequest): InterstitialAd =
            withContext(Dispatchers.Main.immediate) {
                suspendCancellableCoroutine { continuation ->
                    GmaInterstitialAd.load(
                        applicationContext(),
                        adUnitId,
                        request.toAndroid(),
                        object : InterstitialAdLoadCallback() {
                            override fun onAdLoaded(ad: GmaInterstitialAd) {
                                val session = FullScreenAdSession()
                                ad.fullScreenContentCallback = session.contentCallback()
                                ad.onPaidEventListener = session.paidEventListener()
                                continuation.resume(InterstitialAd(ad, session))
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
