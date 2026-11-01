package io.github.corneloaie.admob.spike

import platform.Foundation.NSDecimalNumber
import platform.Foundation.NSError
import platform.darwin.NSObject
import swiftPMImport.io.github.corneloaie.admob.GADAdLoader
import swiftPMImport.io.github.corneloaie.admob.GADAdLoaderAdTypeNative
import swiftPMImport.io.github.corneloaie.admob.GADNativeAd
import swiftPMImport.io.github.corneloaie.admob.GADNativeAdDelegateProtocol
import swiftPMImport.io.github.corneloaie.admob.GADNativeAdLoaderDelegateProtocol
import swiftPMImport.io.github.corneloaie.admob.GADRequest
import swiftPMImport.io.github.corneloaie.admob.GADResponseInfo

/** One paid event, in the shape InfoTraffic forwards to RevenueCat's ad tracker. */
internal data class PaidEvent(val valueMicros: Long, val currency: String, val precision: Long)

/**
 * Spike: InfoTraffic's Swift `AdSlotSession` (iOSApp.swift) as a Kotlin NSObject implementing the GMA loader and
 * native-ad delegate protocols: fill, paid event, impression, click and failure, plus the ResponseInfo fields
 * (response id, network name) RevenueCat needs.
 */
internal class NativeAdSession(
    private val adUnitId: String,
    private val onLoaded: (GADNativeAd) -> Unit,
    private val onImpression: () -> Unit,
    private val onClick: () -> Unit,
    private val onPaid: (PaidEvent) -> Unit,
    private val onFailed: (errorCode: Long) -> Unit,
) : NSObject(), GADNativeAdLoaderDelegateProtocol, GADNativeAdDelegateProtocol {
    // GADAdLoader holds its delegate weakly and the loader must outlive the request: the session keeps both.
    private var loader: GADAdLoader? = null

    fun start() {
        val loader = GADAdLoader(
            adUnitID = adUnitId,
            rootViewController = null,
            adTypes = listOf(GADAdLoaderAdTypeNative),
            options = null,
        )
        loader.delegate = this
        this.loader = loader
        loader.loadRequest(GADRequest())
    }

    override fun adLoader(adLoader: GADAdLoader, didReceiveNativeAd: GADNativeAd) {
        didReceiveNativeAd.delegate = this
        didReceiveNativeAd.paidEventHandler = { adValue ->
            if (adValue != null) {
                val micros = adValue.value.decimalNumberByMultiplyingBy(NSDecimalNumber(longLong = 1_000_000))
                onPaid(PaidEvent(micros.longLongValue, adValue.currencyCode, adValue.precision))
            }
        }
        onLoaded(didReceiveNativeAd)
    }

    override fun adLoader(adLoader: GADAdLoader, didFailToReceiveAdWithError: NSError) {
        onFailed(didFailToReceiveAdWithError.code)
    }

    override fun nativeAdDidRecordImpression(nativeAd: GADNativeAd) {
        onImpression()
    }

    override fun nativeAdDidRecordClick(nativeAd: GADNativeAd) {
        onClick()
    }
}

internal val GADResponseInfo.impressionId: String? get() = responseIdentifier

/** Human-readable fill source ("Google AdMob", "Meta Audience Network"); the adapter class as fallback. */
internal val GADResponseInfo.networkName: String?
    get() {
        val loaded = loadedAdNetworkResponseInfo ?: return null
        return loaded.adSourceName?.takeIf { it.isNotEmpty() } ?: loaded.adNetworkClassName
    }
