package io.github.corneloaie.admob

/**
 * An error the Google Mobile Ads or UMP SDK reported.
 *
 * @property code what went wrong, the same on both platforms.
 * @property platformCode the SDK's own code. Android and iOS number their errors differently: a load with no fill is 3
 *   on Android and 1 on iOS. [domain] says which SDK it came from.
 * @property domain the SDK's error domain, e.g. `com.google.android.libraries.ads.mobile.sdk` or
 *   `com.google.admob`.
 * @property responseInfo for a failed ad load, the response: which mediation sources were tried and how each failed.
 */
public data class AdError(
    val code: AdErrorCode,
    val platformCode: Int,
    val domain: String,
    val message: String,
    val responseInfo: ResponseInfo? = null,
)

/** What went wrong, the same on both platforms. The SDK's own code is [AdError.platformCode]. */
public enum class AdErrorCode {
    /** There was no ad for the request, from AdMob or any mediation source. This is routine: try again later. */
    NO_FILL,

    /** The request didn't reach the server, or no answer came back. */
    NETWORK_ERROR,

    /** The request timed out. */
    TIMEOUT,

    /** The request was invalid: a wrong ad unit id, an invalid argument, or a consent message misconfigured in AdMob. */
    INVALID_REQUEST,

    /** The app's AdMob app id is missing or invalid (the manifest's `APPLICATION_ID`, `GADApplicationIdentifier`). */
    APP_ID_MISSING,

    /** A mediation adapter failed to load or show its ad. */
    MEDIATION_ERROR,

    /** The ad or form was shown already: each shows once. */
    ALREADY_USED,

    /**
     * It can't be shown now: the ad isn't ready, the app isn't in the foreground, the host can't present it, or the
     * consent form is unavailable.
     */
    NOT_READY,

    /** The Ad Inspector opens only on a test device. */
    NOT_IN_TEST_MODE,

    /** An internal error in the SDK. */
    INTERNAL_ERROR,

    /** Any other code: see [AdError.platformCode] and [AdError.domain]. */
    UNKNOWN,
}

/** Thrown by the suspending calls of this library when the SDK reports an [error]. */
public class AdMobException(public val error: AdError) : Exception(error.message)
