package io.github.corneloaie.admob

/**
 * Settings the SDK applies to every ad request (see [AdMob.requestConfiguration]).
 *
 * @property testDeviceIds devices that get test ads and can open the Ad Inspector. The SDK logs a device's id the first
 *   time it requests an ad.
 * @property tagForChildDirectedTreatment true to have requests treated as child-directed (COPPA), false to say they
 *   aren't, null to leave it unspecified.
 * @property tagForUnderAgeOfConsent true to have requests treated as coming from a user under the age of consent
 *   (GDPR), false to say they aren't, null to leave it unspecified.
 * @property maxAdContentRating the most mature ad content to serve; null leaves it to the AdMob settings.
 */
public data class RequestConfiguration(
    val testDeviceIds: List<String> = emptyList(),
    val tagForChildDirectedTreatment: Boolean? = null,
    val tagForUnderAgeOfConsent: Boolean? = null,
    val maxAdContentRating: MaxAdContentRating? = null,
)

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
