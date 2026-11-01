package io.github.corneloaie.admob

import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resumeWithException
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAd as GmaInterstitialAd

public actual class InterstitialAd internal constructor(
    /** The GMA Next-Gen SDK's ad. */
    public val android: GmaInterstitialAd,
    private val session: FullScreenAdSession,
) {
    public actual val adUnitId: String get() = android.adUnitId
    public actual val responseInfo: ResponseInfo? get() = android.getResponseInfo().toCommon()
    public actual val events: Flow<AdEvent> get() = session.events.flow

    public actual suspend fun show(host: AdHost) {
        withContext(Dispatchers.Main.immediate) { session.show { android.show(host) } }
    }

    public actual companion object {
        public actual suspend fun load(adUnitId: String, request: AdRequest): InterstitialAd =
            withContext(Dispatchers.Main.immediate) {
                awaitStartedSdk()
                suspendCancellableCoroutine { continuation ->
                    GmaInterstitialAd.load(
                        request.toAndroid(adUnitId),
                        object : AdLoadCallback<GmaInterstitialAd> {
                            // On a background thread; the load returns on the main one.
                            override fun onAdLoaded(ad: GmaInterstitialAd) {
                                val session = FullScreenAdSession()
                                ad.adEventCallback = FullScreenEventCallback(session)
                                continuation.resume(InterstitialAd(ad, session)) { _, loaded, _ ->
                                    loaded.android.destroy()
                                }
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
