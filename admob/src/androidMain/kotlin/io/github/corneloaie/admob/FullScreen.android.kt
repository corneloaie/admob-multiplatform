package io.github.corneloaie.admob

import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdValue as GmaAdValue

/**
 * The SDK's events for one full-screen ad, routed to [session] on the main thread, where the session lives. Set at load
 * time, before the app has the ad.
 */
internal class FullScreenEventCallback(private val session: FullScreenAdSession) :
    InterstitialAdEventCallback,
    RewardedAdEventCallback {
    override fun onAdShowedFullScreenContent() = postToMainThread { session.onShown() }

    override fun onAdImpression() = postToMainThread { session.onImpression() }

    override fun onAdClicked() = postToMainThread { session.onClick() }

    override fun onAdPaid(value: GmaAdValue) = postToMainThread { session.onPaid(value.toCommon()) }

    override fun onAdDismissedFullScreenContent() = postToMainThread { session.onDismissed() }

    override fun onAdFailedToShowFullScreenContent(fullScreenContentError: FullScreenContentError) =
        postToMainThread { session.onFailedToShow(fullScreenContentError.toAdError()) }
}
