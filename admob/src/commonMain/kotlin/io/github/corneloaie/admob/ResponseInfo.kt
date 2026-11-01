package io.github.corneloaie.admob

/**
 * The ad server's response behind an ad, or behind a failed load.
 *
 * @property responseId the response's id, the only per-fill identifier the SDK exposes (RevenueCat's impression id).
 * @property mediationAdapterClassName the class of the adapter that filled the request.
 * @property loadedAdapterResponseInfo the mediation source that filled the request.
 * @property adapterResponses every mediation source the SDK tried, in order.
 */
public data class ResponseInfo(
    val responseId: String?,
    val mediationAdapterClassName: String?,
    val loadedAdapterResponseInfo: AdapterResponseInfo?,
    val adapterResponses: List<AdapterResponseInfo>,
)

/**
 * One mediation source's part in a response. The SDKs report a missing value as an empty string; here it is null.
 *
 * @property adSourceName the source's readable name, e.g. `AdMob Network` or `Meta Audience Network`.
 * @property latencyMillis how long the source took to answer.
 * @property error why the source didn't fill, if it didn't.
 */
public data class AdapterResponseInfo(
    val adapterClassName: String,
    val adSourceName: String?,
    val adSourceId: String?,
    val adSourceInstanceName: String?,
    val adSourceInstanceId: String?,
    val latencyMillis: Long,
    val error: AdError?,
)

internal fun String?.orNullIfEmpty(): String? = this?.takeIf { it.isNotEmpty() }
