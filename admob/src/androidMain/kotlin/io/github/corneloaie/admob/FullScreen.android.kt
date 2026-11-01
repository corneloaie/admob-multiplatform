package io.github.corneloaie.admob

import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.OnPaidEventListener
import com.google.android.gms.ads.AdError as GmaAdError

/** The SDK's full-screen callbacks for one ad, routed to [FullScreenAdSession]. Set at load time, before [show]. */
internal fun FullScreenAdSession.contentCallback(): FullScreenContentCallback = object : FullScreenContentCallback() {
    override fun onAdShowedFullScreenContent() = onShown()

    override fun onAdImpression() = onImpression()

    override fun onAdClicked() = onClick()

    override fun onAdDismissedFullScreenContent() = onDismissed()

    override fun onAdFailedToShowFullScreenContent(error: GmaAdError) =
        onFailedToShow(error.toAdError(AdErrorSource.SHOW))
}

internal fun FullScreenAdSession.paidEventListener(): OnPaidEventListener =
    OnPaidEventListener { value -> onPaid(value.toCommon()) }
