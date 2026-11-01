package io.github.corneloaie.admob

/**
 * Settings the SDK applies to every ad request (see [AdMob.requestConfiguration]).
 *
 * @property testDeviceIds devices that get test ads and can open the Ad Inspector. The SDK logs a device's id the first
 *   time it requests an ad.
 * @property ageRestrictedTreatment the age treatment every request asks for; null asks for none. It replaces the
 *   child-directed (COPPA) and under-age-of-consent tags, which both SDKs deprecate. Setting it certifies to Google
 *   that the treatment is accurate for the app's users.
 * @property maxAdContentRating the most mature ad content to serve; null leaves it to the AdMob settings.
 */
public data class RequestConfiguration(
    val testDeviceIds: List<String> = emptyList(),
    val ageRestrictedTreatment: AgeRestrictedTreatment? = null,
    val maxAdContentRating: MaxAdContentRating? = null,
)

/** An age treatment for ad requests (see the AdMob targeting guide, "Set the age treatment"). */
public enum class AgeRestrictedTreatment {
    /** Child treatment, e.g. for an app or a user that COPPA applies to. */
    CHILD,

    /** Teen treatment, e.g. for a user under the age of consent. */
    TEEN,
}

/** The maximum ad content rating, from the most to the least restrictive. */
public enum class MaxAdContentRating {
    /** General audiences. */
    G,

    /** Parental guidance. */
    PG,

    /** Teen. */
    T,

    /** Mature audiences. */
    MA,
}
