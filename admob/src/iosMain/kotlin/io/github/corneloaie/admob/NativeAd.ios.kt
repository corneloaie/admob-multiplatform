package io.github.corneloaie.admob

import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSError
import platform.darwin.NSObject
import swiftPMImport.io.github.corneloaie.admob.GADAdLoader
import swiftPMImport.io.github.corneloaie.admob.GADAdLoaderAdTypeNative
import swiftPMImport.io.github.corneloaie.admob.GADNativeAd
import swiftPMImport.io.github.corneloaie.admob.GADNativeAdDelegateProtocol
import swiftPMImport.io.github.corneloaie.admob.GADNativeAdImage
import swiftPMImport.io.github.corneloaie.admob.GADNativeAdLoaderDelegateProtocol
import kotlin.coroutines.resumeWithException

public actual class NativeAd internal constructor(
    /**
     * The Google Mobile Ads SDK's ad, to set as a `GADNativeAdView`'s `nativeAd` once the view is laid out (see
     * [AttachRegisteringNativeAdView]).
     */
    public val ios: GADNativeAd,
    public actual val adUnitId: String,
    private val adEvents: AdEvents,
) {
    public actual val headline: String? get() = ios.headline
    public actual val body: String? get() = ios.body
    public actual val callToAction: String? get() = ios.callToAction
    public actual val icon: NativeAdImage? get() = ios.icon?.toCommon()
    public actual val images: List<NativeAdImage>
        get() = ios.images.orEmpty().mapNotNull { (it as? GADNativeAdImage)?.toCommon() }
    public actual val advertiser: String? get() = ios.advertiser
    public actual val store: String? get() = ios.store
    public actual val price: String? get() = ios.price
    public actual val starRating: Double? get() = ios.starRating?.doubleValue
    public actual val mediaContent: NativeAdMediaContent? get() = ios.mediaContent.toCommon()
    public actual val responseInfo: ResponseInfo? get() = ios.responseInfo.toCommon()
    public actual val events: Flow<AdEvent> get() = adEvents.flow

    public actual fun destroy() {
        adEvents.close()
        ios.paidEventHandler = null
        ios.delegate = null
        retainDelegate(ios, null)
    }
}

// Each request until it ends: GADAdLoader holds its delegate weakly. Main thread only.
private val pendingLoads = mutableSetOf<NativeAdLoad>()

internal actual suspend fun loadPlatformNativeAd(adUnitId: String, options: NativeAdOptions, request: AdRequest): NativeAd =
    suspendCancellableCoroutine { continuation -> NativeAdLoad(adUnitId, continuation).start(options, request) }

/** One request: a GADAdLoader, which must outlive it, and its delegate. */
private class NativeAdLoad(
    private val adUnitId: String,
    private val continuation: CancellableContinuation<NativeAd>,
) : NSObject(), GADNativeAdLoaderDelegateProtocol {
    private var loader: GADAdLoader? = null

    fun start(options: NativeAdOptions, request: AdRequest) {
        val loader = GADAdLoader(
            adUnitID = adUnitId,
            rootViewController = null,
            adTypes = listOf(GADAdLoaderAdTypeNative),
            options = options.toIos(),
        )
        loader.delegate = this
        this.loader = loader
        pendingLoads.add(this)
        loader.loadRequest(request.toIos())
    }

    override fun adLoader(adLoader: GADAdLoader, didReceiveNativeAd: GADNativeAd) {
        end()
        continuation.resume(wrap(didReceiveNativeAd)) { _, ad, _ -> ad.destroy() }
    }

    override fun adLoader(adLoader: GADAdLoader, didFailToReceiveAdWithError: NSError) {
        end()
        continuation.resumeWithException(AdMobException(didFailToReceiveAdWithError.toAdError(AdErrorSource.LOAD)))
    }

    private fun end() {
        pendingLoads.remove(this)
        loader = null
    }

    private fun wrap(ad: GADNativeAd): NativeAd {
        val events = AdEvents()
        val delegate = NativeAdEvents(events)
        ad.delegate = delegate
        retainDelegate(ad, delegate)
        // Before the app has the ad: the SDK reports the paid event only to a handler set at impression time. It
        // captures the events alone, never the ad or its wrapper: Kotlin/Native can't free a cycle through an Obj-C
        // object.
        ad.paidEventHandler = { value -> if (value != null) events.emit(AdEvent.Paid(value.toCommon())) }
        return NativeAd(ad, adUnitId, events)
    }
}

private class NativeAdEvents(private val events: AdEvents) : NSObject(), GADNativeAdDelegateProtocol {
    override fun nativeAdDidRecordImpression(nativeAd: GADNativeAd) = events.emit(AdEvent.Impression)

    override fun nativeAdDidRecordClick(nativeAd: GADNativeAd) = events.emit(AdEvent.Click)
}
