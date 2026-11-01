package io.github.corneloaie.admob

import kotlinx.coroutines.flow.Flow

/**
 * A loaded native ad (see [NativeAdLoader]): its assets, its response and its events.
 *
 * The app renders it natively, with the platform ad: `android`, a
 * `com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd`, goes into a `NativeAdView`; `ios`, a `GADNativeAd`,
 * into a `GADNativeAdView` (see `AttachRegisteringNativeAdView`). AdMob's policies want the headline shown, and an "Ad"
 * attribution.
 *
 * Call [destroy] once the ad is off screen for good.
 */
public expect class NativeAd {
    /** The ad unit the ad was loaded for. */
    public val adUnitId: String

    /** The headline. AdMob requires it to be shown. */
    public val headline: String?

    public val body: String?

    /** The button text, e.g. "Install". */
    public val callToAction: String?

    public val icon: NativeAdImage?

    public val images: List<NativeAdImage>

    public val advertiser: String?

    /** For an app install ad, the store it is in, e.g. "Google Play". */
    public val store: String?

    /** For an app install ad, its price, e.g. "Free". */
    public val price: String?

    /** For an app install ad, its store rating, from 0 to 5. */
    public val starRating: Double?

    /** The main image or video, which the platform's media view renders. */
    public val mediaContent: NativeAdMediaContent?

    public val responseInfo: ResponseInfo?

    /**
     * The ad's [AdEvent.Impression], [AdEvent.Click] and [AdEvent.Paid] events. Every collector gets all of them from the
     * start, however late it starts; the flow completes when the ad is destroyed.
     */
    public val events: Flow<AdEvent>

    /** Releases the ad and completes [events]. Its views must not show it any more. */
    public fun destroy()
}

/**
 * An image asset of a [NativeAd]. The decoded image (a `Drawable`, a `UIImage`) is on the platform ad.
 *
 * @property scale the image's pixels per point (dp).
 */
public data class NativeAdImage(val uri: String?, val scale: Double)

/**
 * The main media of a [NativeAd].
 *
 * @property aspectRatio width / height, or 0 when unknown.
 * @property durationSeconds the video's length; 0 for an image.
 */
public data class NativeAdMediaContent(
    val aspectRatio: Double,
    val hasVideoContent: Boolean,
    val durationSeconds: Double,
)
