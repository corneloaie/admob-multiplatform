package io.github.corneloaie.admob

import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.LoadAdError
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resumeWithException
import com.google.android.gms.ads.nativead.NativeAd as GmaNativeAd

public actual class NativeAd internal constructor(
    /** The Google Mobile Ads SDK's ad, to register with a `NativeAdView` (`NativeAdView.setNativeAd`). */
    public val android: GmaNativeAd,
    public actual val adUnitId: String,
    private val adEvents: AdEvents,
) {
    public actual val headline: String? get() = android.headline
    public actual val body: String? get() = android.body
    public actual val callToAction: String? get() = android.callToAction
    public actual val icon: NativeAdImage? get() = android.icon?.toCommon()
    public actual val images: List<NativeAdImage> get() = android.images.map { it.toCommon() }
    public actual val advertiser: String? get() = android.advertiser
    public actual val store: String? get() = android.store
    public actual val price: String? get() = android.price
    public actual val starRating: Double? get() = android.starRating
    public actual val mediaContent: NativeAdMediaContent? get() = android.mediaContent?.toCommon()
    public actual val responseInfo: ResponseInfo? get() = android.responseInfo?.toCommon()
    public actual val events: Flow<AdEvent> get() = adEvents.flow

    public actual fun destroy() {
        adEvents.close()
        android.setOnPaidEventListener(null)
        android.destroy()
    }
}

internal actual suspend fun loadPlatformNativeAd(adUnitId: String, options: NativeAdOptions, request: AdRequest): NativeAd =
    suspendCancellableCoroutine { continuation ->
        // One AdLoader per request: its AdListener reports the impression and click of the one ad it loads.
        val events = AdEvents()
        val loader = AdLoader.Builder(applicationContext(), adUnitId)
            .forNativeAd { ad ->
                // Before the app has the ad: the SDK reports the paid event only to a listener set at impression time.
                ad.setOnPaidEventListener { value -> events.emit(AdEvent.Paid(value.toCommon())) }
                continuation.resume(NativeAd(ad, adUnitId, events)) { _, loaded, _ -> loaded.destroy() }
            }
            .withAdListener(
                object : AdListener() {
                    override fun onAdFailedToLoad(error: LoadAdError) {
                        continuation.resumeWithException(AdMobException(error.toAdError()))
                    }

                    override fun onAdImpression() = events.emit(AdEvent.Impression)

                    override fun onAdClicked() = events.emit(AdEvent.Click)
                },
            )
            .withNativeAdOptions(options.toAndroid())
            .build()
        loader.loadAd(request.toAndroid())
    }
