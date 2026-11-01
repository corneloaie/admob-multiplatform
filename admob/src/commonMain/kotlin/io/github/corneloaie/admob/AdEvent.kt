package io.github.corneloaie.admob

/**
 * What happened to a loaded ad, in the order the SDK reported it. Native ads report [Impression], [Click] and [Paid];
 * full-screen ads also report [Shown], then [Dismissed] or [FailedToShow], and a rewarded ad [RewardEarned].
 */
public sealed interface AdEvent {
    /** The SDK recorded an impression: the ad was on screen. RevenueCat calls this "displayed". */
    public data object Impression : AdEvent

    /** The user tapped the ad. RevenueCat calls this "opened". */
    public data object Click : AdEvent

    /** What the impression earned (the paid event). */
    public data class Paid(val value: AdValue) : AdEvent

    /** A full-screen ad is showing. */
    public data object Shown : AdEvent

    /** A full-screen ad was closed. Nothing follows it. */
    public data object Dismissed : AdEvent

    /** A full-screen ad couldn't be shown. Nothing follows it. */
    public data class FailedToShow(val error: AdError) : AdEvent

    /** The user earned a rewarded ad's reward. [Dismissed] follows when they close the ad. */
    public data class RewardEarned(val reward: RewardItem) : AdEvent
}
