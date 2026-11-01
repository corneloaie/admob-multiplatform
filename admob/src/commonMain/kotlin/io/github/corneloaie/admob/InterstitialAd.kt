package io.github.corneloaie.admob

import kotlinx.coroutines.flow.Flow

/**
 * A loaded interstitial ad: a full-screen ad shown at a natural break. It shows once. Its platform ad is `android`
 * (`com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAd`) or `ios` (`GADInterstitialAd`).
 */
public expect class InterstitialAd {
    public val adUnitId: String

    public val responseInfo: ResponseInfo?

    /**
     * The ad's events, from [AdEvent.Shown] to [AdEvent.Dismissed] or [AdEvent.FailedToShow], with which the flow
     * completes. Every collector gets all of them from the start, however late it starts.
     */
    public val events: Flow<AdEvent>

    /**
     * Shows the ad over [host] and returns when the user closes it.
     *
     * @throws AdMobException when it can't be shown, e.g. [AdErrorCode.ALREADY_USED].
     */
    public suspend fun show(host: AdHost)

    public companion object {
        /**
         * Requests an ad and returns it once it has loaded. Its events are wired before it is returned.
         *
         * @throws AdMobException when the request fails, e.g. [AdErrorCode.NO_FILL].
         */
        public suspend fun load(adUnitId: String, request: AdRequest = AdRequest()): InterstitialAd
    }
}
