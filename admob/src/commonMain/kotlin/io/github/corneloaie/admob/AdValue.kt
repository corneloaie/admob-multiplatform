package io.github.corneloaie.admob

/**
 * What one impression earned: the SDK's paid event. The SDK reports it only once impression-level ad revenue is on in
 * the AdMob account settings.
 *
 * @property valueMicros the value in millionths of [currencyCode]: 1_500_000 is 1.5.
 * @property currencyCode the ISO 4217 currency code, e.g. `USD`.
 */
public data class AdValue(
    val valueMicros: Long,
    val currencyCode: String,
    val precision: AdValuePrecision,
)

/** How exact an [AdValue] is. */
public enum class AdValuePrecision {
    UNKNOWN,

    /** Estimated from aggregated data. */
    ESTIMATED,

    /** Provided by the publisher, e.g. a manual CPM in a mediation group. */
    PUBLISHER_PROVIDED,

    /** The exact value paid for this ad. */
    PRECISE,
}
