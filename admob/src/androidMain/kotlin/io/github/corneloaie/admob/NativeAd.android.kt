package io.github.corneloaie.admob

import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoader
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoaderCallback
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resumeWithException
import com.google.android.libraries.ads.mobile.sdk.common.AdValue as GmaAdValue
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd as GmaNativeAd

public actual class NativeAd internal constructor(
    /** The GMA Next-Gen SDK's ad, to register with a `NativeAdView` (`NativeAdView.registerNativeAd`). */
    public val android: GmaNativeAd,
    private val adEvents: AdEvents,
) {
    public actual val adUnitId: String get() = android.adUnitId
    public actual val headline: String? get() = android.headline
    public actual val body: String? get() = android.body
    public actual val callToAction: String? get() = android.callToAction
    public actual val icon: NativeAdImage? get() = android.icon?.toCommon()

    /** The Next-Gen SDK has one image at most. */
    public actual val images: List<NativeAdImage> get() = listOfNotNull(android.image?.toCommon())
    public actual val advertiser: String? get() = android.advertiser
    public actual val store: String? get() = android.store
    public actual val price: String? get() = android.price
    public actual val starRating: Double? get() = android.starRating
    public actual val mediaContent: NativeAdMediaContent? get() = android.mediaContent.toCommon()
    public actual val responseInfo: ResponseInfo? get() = android.getResponseInfo().toCommon()
    public actual val events: Flow<AdEvent> get() = adEvents.flow

    public actual fun destroy() {
        adEvents.close()
        android.adEventCallback = null
        android.destroy()
    }
}

internal actual suspend fun loadPlatformNativeAd(adUnitId: String, options: NativeAdOptions, request: AdRequest): NativeAd {
    awaitStartedSdk()
    return suspendCancellableCoroutine { continuation ->
        NativeAdLoader.load(
            nativeAdRequest(adUnitId, options, request),
            object : NativeAdLoaderCallback {
                // On a background thread. The events are set before the app has the ad: the SDK reports the paid event
                // only to a callback in place at impression time.
                override fun onNativeAdLoaded(nativeAd: GmaNativeAd) {
                    val events = AdEvents()
                    nativeAd.adEventCallback = events.nativeAdEventCallback()
                    continuation.resume(NativeAd(nativeAd, events)) { _, loaded, _ -> loaded.android.destroy() }
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    continuation.resumeWithException(AdMobException(adError.toAdError()))
                }
            },
        )
    }
}

private fun AdEvents.nativeAdEventCallback() = object : NativeAdEventCallback {
    override fun onAdImpression() = postToMainThread { emit(AdEvent.Impression) }

    override fun onAdClicked() = postToMainThread { emit(AdEvent.Click) }

    override fun onAdPaid(value: GmaAdValue) = postToMainThread { emit(AdEvent.Paid(value.toCommon())) }
}
