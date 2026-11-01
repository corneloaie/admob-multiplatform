package io.github.corneloaie.admob

import platform.Foundation.NSError
import platform.darwin.NSObject
import swiftPMImport.io.github.corneloaie.admob.GADAdValue
import swiftPMImport.io.github.corneloaie.admob.GADFullScreenContentDelegateProtocol
import swiftPMImport.io.github.corneloaie.admob.GADFullScreenPresentingAdProtocol

/** Routes the SDK's full-screen callbacks for this ad to [session]. Set at load time, before [FullScreenAdSession.show]. */
internal fun GADFullScreenPresentingAdProtocol.attach(session: FullScreenAdSession) {
    val delegate = FullScreenDelegate(session)
    fullScreenContentDelegate = delegate
    retainDelegate(this, delegate)
}

/** The ad's paid event handler. It captures the session alone: Kotlin/Native can't free a cycle through the ad. */
internal fun FullScreenAdSession.paidEventHandler(): (GADAdValue?) -> Unit =
    { value -> if (value != null) onPaid(value.toCommon()) }

private class FullScreenDelegate(private val session: FullScreenAdSession) :
    NSObject(),
    GADFullScreenContentDelegateProtocol {
    override fun adWillPresentFullScreenContent(ad: GADFullScreenPresentingAdProtocol) = session.onShown()

    override fun adDidRecordImpression(ad: GADFullScreenPresentingAdProtocol) = session.onImpression()

    override fun adDidRecordClick(ad: GADFullScreenPresentingAdProtocol) = session.onClick()

    override fun adDidDismissFullScreenContent(ad: GADFullScreenPresentingAdProtocol) = session.onDismissed()

    override fun ad(ad: GADFullScreenPresentingAdProtocol, didFailToPresentFullScreenContentWithError: NSError) =
        session.onFailedToShow(didFailToPresentFullScreenContentWithError.toAdError(AdErrorSource.SHOW))
}

/** A load's result as an [AdError]: the SDK's error, or one for a completion that brought neither ad nor error. */
internal fun NSError?.toLoadError(): AdError = this?.toAdError(AdErrorSource.LOAD)
    ?: AdError(AdErrorCode.UNKNOWN, platformCode = 0, domain = "", message = "The SDK returned neither an ad nor an error")
