package io.github.corneloaie.admob

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import swiftPMImport.io.github.corneloaie.admob.GADInterstitialAd
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

public actual class InterstitialAd internal constructor(
    /** The Google Mobile Ads SDK's ad. */
    public val ios: GADInterstitialAd,
    private val session: FullScreenAdSession,
) {
    public actual val adUnitId: String get() = ios.adUnitID
    public actual val responseInfo: ResponseInfo? get() = ios.responseInfo.toCommon()
    public actual val events: Flow<AdEvent> get() = session.events.flow

    public actual suspend fun show(host: AdHost) {
        withContext(Dispatchers.Main.immediate) { session.show { ios.presentFromRootViewController(host) } }
    }

    public actual companion object {
        public actual suspend fun load(adUnitId: String, request: AdRequest): InterstitialAd =
            withContext(Dispatchers.Main.immediate) {
                suspendCancellableCoroutine { continuation ->
                    GADInterstitialAd.loadWithAdUnitID(adUnitId, request.toIos()) { ad, error ->
                        if (ad != null) {
                            val session = FullScreenAdSession()
                            ad.attach(session)
                            ad.paidEventHandler = session.paidEventHandler()
                            continuation.resume(InterstitialAd(ad, session))
                        } else {
                            continuation.resumeWithException(AdMobException(error.toLoadError()))
                        }
                    }
                }
            }
    }
}
