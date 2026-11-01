package io.github.corneloaie.admob

import kotlinx.coroutines.flow.Flow

/**
 * A loaded rewarded ad: a full-screen ad the user chooses to watch for a reward. It shows once. Its platform ad is
 * `android` (`com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAd`) or `ios` (`GADRewardedAd`).
 */
public expect class RewardedAd {
    public val adUnitId: String

    public val responseInfo: ResponseInfo?

    /** The reward the ad unit grants, as set up in AdMob. */
    public val rewardItem: RewardItem

    /**
     * The ad's events, from [AdEvent.Shown] to [AdEvent.Dismissed] or [AdEvent.FailedToShow], with which the flow
     * completes. Every collector gets all of them from the start, however late it starts.
     */
    public val events: Flow<AdEvent>

    /** Has AdMob's server-side verification callback carry [options]. Set it before [show]. */
    public fun setServerSideVerificationOptions(options: ServerSideVerificationOptions)

    /**
     * Shows the ad over [host] and returns when the user closes it: the reward if they earned it, else null.
     *
     * @throws AdMobException when it can't be shown, e.g. [AdErrorCode.ALREADY_USED].
     */
    public suspend fun show(host: AdHost): RewardItem?

    public companion object {
        /**
         * Requests an ad and returns it once it has loaded. Its events are wired before it is returned.
         *
         * @throws AdMobException when the request fails, e.g. [AdErrorCode.NO_FILL].
         */
        public suspend fun load(adUnitId: String, request: AdRequest = AdRequest()): RewardedAd
    }
}

/** A rewarded ad's reward, e.g. 10 coins. */
public data class RewardItem(val type: String, val amount: Int)

/** What AdMob's server-side verification callback carries, to tell the app's server who earned a reward. */
public data class ServerSideVerificationOptions(val userId: String? = null, val customData: String? = null)
