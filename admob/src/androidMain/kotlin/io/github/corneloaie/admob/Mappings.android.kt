package io.github.corneloaie.admob

import android.content.Context
import com.google.android.libraries.ads.mobile.sdk.common.AdInspectorError
import com.google.android.libraries.ads.mobile.sdk.common.AdSourceResponseInfo
import com.google.android.libraries.ads.mobile.sdk.common.BaseAdRequestBuilder
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.common.Image
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.common.MediationAdError
import com.google.android.libraries.ads.mobile.sdk.common.PrecisionType
import com.google.android.libraries.ads.mobile.sdk.common.VideoOptions
import com.google.android.libraries.ads.mobile.sdk.nativead.MediaContent
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdRequest
import com.google.android.ump.ConsentInformation
import com.google.android.ump.FormError
import com.google.android.libraries.ads.mobile.sdk.common.AdChoicesPlacement as GmaAdChoicesPlacement
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest as GmaAdRequest
import com.google.android.libraries.ads.mobile.sdk.common.AdValue as GmaAdValue
import com.google.android.libraries.ads.mobile.sdk.common.AgeRestrictedTreatment as GmaAgeRestrictedTreatment
import com.google.android.libraries.ads.mobile.sdk.common.RequestConfiguration as GmaRequestConfiguration
import com.google.android.libraries.ads.mobile.sdk.common.ResponseInfo as GmaResponseInfo
import com.google.android.libraries.ads.mobile.sdk.initialization.AdapterStatus as GmaAdapterStatus
import com.google.android.libraries.ads.mobile.sdk.initialization.InitializationStatus as GmaInitializationStatus
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd as GmaNativeAd
import com.google.android.ump.ConsentDebugSettings as UmpConsentDebugSettings
import com.google.android.ump.ConsentRequestParameters as UmpConsentRequestParameters

// Every SDK value is mapped by its SDK constant, never by its number: Android and iOS number some of them differently.
// The `else` branches take the constants a newer SDK may add, as apps can raise the SDK version.

internal fun RequestConfiguration.toAndroid(): GmaRequestConfiguration = GmaRequestConfiguration.Builder()
    .setTestDeviceIds(testDeviceIds)
    .setAgeRestrictedTreatment(
        when (ageRestrictedTreatment) {
            AgeRestrictedTreatment.CHILD -> GmaAgeRestrictedTreatment.CHILD
            AgeRestrictedTreatment.TEEN -> GmaAgeRestrictedTreatment.TEEN
            null -> GmaAgeRestrictedTreatment.UNSPECIFIED
        },
    )
    .setMaxAdContentRating(
        when (maxAdContentRating) {
            MaxAdContentRating.G -> GmaRequestConfiguration.MaxAdContentRating.MAX_AD_CONTENT_RATING_G
            MaxAdContentRating.PG -> GmaRequestConfiguration.MaxAdContentRating.MAX_AD_CONTENT_RATING_PG
            MaxAdContentRating.T -> GmaRequestConfiguration.MaxAdContentRating.MAX_AD_CONTENT_RATING_T
            MaxAdContentRating.MA -> GmaRequestConfiguration.MaxAdContentRating.MAX_AD_CONTENT_RATING_MA
            null -> GmaRequestConfiguration.MaxAdContentRating.MAX_AD_CONTENT_RATING_UNSPECIFIED
        },
    )
    .build()

internal fun GmaInitializationStatus.toCommon(): InitializationStatus =
    InitializationStatus(adapterStatusMap.mapValues { (_, status) -> status.toCommon() })

private fun GmaAdapterStatus.toCommon() = AdapterStatus(
    // An adapter can bid once its start is complete; it may also be starting still, or have timed out or failed.
    state = when (initializationState) {
        GmaAdapterStatus.InitializationState.COMPLETE -> AdapterState.READY
        else -> AdapterState.NOT_READY
    },
    description = description,
    latencyMillis = latency.toLong(),
)

internal fun AdRequest.toAndroid(adUnitId: String): GmaAdRequest =
    GmaAdRequest.Builder(adUnitId).withTargeting(this).build()

internal fun nativeAdRequest(adUnitId: String, options: NativeAdOptions, request: AdRequest): NativeAdRequest =
    NativeAdRequest.Builder(adUnitId, listOf(GmaNativeAd.NativeAdType.NATIVE))
        .withTargeting(request)
        .apply {
            options.mediaAspectRatio?.let { setMediaAspectRatio(it.toAndroid()) }
            options.adChoicesPlacement?.let { setAdChoicesPlacement(it.toAndroid()) }
            options.startMuted?.let { setVideoOptions(VideoOptions.Builder().setStartMuted(it).build()) }
        }
        .build()

/** The targeting of [request], which every Next-Gen request builder takes. */
private fun <B : BaseAdRequestBuilder<B>> B.withTargeting(request: AdRequest): B = apply {
    request.keywords.forEach { addKeyword(it) }
    request.contentUrl?.let { setContentUrl(it) }
    if (request.neighboringContentUrls.isNotEmpty()) setNeighboringContentUrls(request.neighboringContentUrls.toSet())
}

// The Next-Gen SDK's errors carry an enum and no domain: its package stands in for one.
private const val GMA_ERROR_DOMAIN = "com.google.android.libraries.ads.mobile.sdk"

internal fun LoadAdError.toAdError(): AdError = AdError(
    code = code.toCommon(),
    platformCode = code.value,
    domain = GMA_ERROR_DOMAIN,
    message = message,
    responseInfo = responseInfo?.toCommon(),
)

internal fun LoadAdError.ErrorCode.toCommon(): AdErrorCode = when (this) {
    LoadAdError.ErrorCode.NO_FILL -> AdErrorCode.NO_FILL
    LoadAdError.ErrorCode.NETWORK_ERROR -> AdErrorCode.NETWORK_ERROR
    LoadAdError.ErrorCode.TIMEOUT -> AdErrorCode.TIMEOUT
    LoadAdError.ErrorCode.INVALID_REQUEST,
    LoadAdError.ErrorCode.NOT_FOUND,
    LoadAdError.ErrorCode.REQUEST_ID_MISMATCH,
    LoadAdError.ErrorCode.INVALID_AD_RESPONSE,
    -> AdErrorCode.INVALID_REQUEST
    LoadAdError.ErrorCode.APP_ID_MISSING -> AdErrorCode.APP_ID_MISSING
    LoadAdError.ErrorCode.AD_RESPONSE_ALREADY_USED -> AdErrorCode.ALREADY_USED
    LoadAdError.ErrorCode.INTERNAL_ERROR -> AdErrorCode.INTERNAL_ERROR
    else -> AdErrorCode.UNKNOWN
}

internal fun FullScreenContentError.toAdError(): AdError =
    AdError(code = code.toCommon(), platformCode = code.value, domain = GMA_ERROR_DOMAIN, message = message)

internal fun FullScreenContentError.ErrorCode.toCommon(): AdErrorCode = when (this) {
    FullScreenContentError.ErrorCode.AD_REUSED -> AdErrorCode.ALREADY_USED
    FullScreenContentError.ErrorCode.APP_NOT_FOREGROUND -> AdErrorCode.NOT_READY
    FullScreenContentError.ErrorCode.MEDIATION_SHOW_ERROR -> AdErrorCode.MEDIATION_ERROR
    FullScreenContentError.ErrorCode.INTERNAL_ERROR -> AdErrorCode.INTERNAL_ERROR
    else -> AdErrorCode.UNKNOWN
}

internal fun AdInspectorError.toAdError(): AdError =
    AdError(code = code.toCommon(), platformCode = code.value, domain = GMA_ERROR_DOMAIN, message = message)

internal fun AdInspectorError.ErrorCode.toCommon(): AdErrorCode = when (this) {
    AdInspectorError.ErrorCode.NOT_IN_TEST_MODE -> AdErrorCode.NOT_IN_TEST_MODE
    AdInspectorError.ErrorCode.ALREADY_OPEN -> AdErrorCode.NOT_READY
    AdInspectorError.ErrorCode.FAILED_TO_LOAD -> AdErrorCode.NETWORK_ERROR
    AdInspectorError.ErrorCode.INTERNAL_ERROR -> AdErrorCode.INTERNAL_ERROR
    else -> AdErrorCode.UNKNOWN
}

/** A mediation source's error: the SDK's own, in its load error numbering, or an adapter's, numbered by the adapter. */
internal fun MediationAdError.toAdError(): AdError = AdError(
    code = if (domain == MediationAdError.GOOGLE_MOBILE_ADS_DOMAIN) {
        when (code) {
            GmaAdRequest.ErrorCode.NO_FILL, GmaAdRequest.ErrorCode.MEDIATION_NO_FILL -> AdErrorCode.NO_FILL
            GmaAdRequest.ErrorCode.NETWORK_ERROR -> AdErrorCode.NETWORK_ERROR
            GmaAdRequest.ErrorCode.INVALID_REQUEST, GmaAdRequest.ErrorCode.INVALID_AD_STRING -> AdErrorCode.INVALID_REQUEST
            GmaAdRequest.ErrorCode.APP_ID_MISSING -> AdErrorCode.APP_ID_MISSING
            GmaAdRequest.ErrorCode.INTERNAL_ERROR -> AdErrorCode.INTERNAL_ERROR
            else -> AdErrorCode.UNKNOWN
        }
    } else {
        AdErrorCode.MEDIATION_ERROR
    },
    platformCode = code,
    domain = domain,
    message = message,
)

internal fun FormError.toAdError(): AdError = AdError(
    code = consentErrorCodeOf(errorCode),
    platformCode = errorCode,
    domain = UMP_ERROR_DOMAIN,
    message = message,
)

// UMP's FormError carries no domain of its own.
private const val UMP_ERROR_DOMAIN = "com.google.android.ump"

internal fun consentErrorCodeOf(code: Int): AdErrorCode = when (code) {
    FormError.ErrorCode.INTERNET_ERROR -> AdErrorCode.NETWORK_ERROR
    FormError.ErrorCode.TIME_OUT -> AdErrorCode.TIMEOUT
    FormError.ErrorCode.INVALID_OPERATION -> AdErrorCode.INVALID_REQUEST
    FormError.ErrorCode.INTERNAL_ERROR -> AdErrorCode.INTERNAL_ERROR
    else -> AdErrorCode.UNKNOWN
}

internal fun GmaResponseInfo.toCommon(): ResponseInfo = ResponseInfo(
    responseId = responseId,
    mediationAdapterClassName = adapterClassName,
    loadedAdapterResponseInfo = loadedAdSourceResponseInfo?.toCommon(),
    adapterResponses = adSourceResponses.map { it.toCommon() },
)

private fun AdSourceResponseInfo.toCommon() = AdapterResponseInfo(
    adapterClassName = adapterClassName,
    adSourceName = name.orNullIfEmpty(),
    adSourceId = id.orNullIfEmpty(),
    adSourceInstanceName = instanceName.orNullIfEmpty(),
    adSourceInstanceId = instanceId.orNullIfEmpty(),
    latencyMillis = latencyMillis,
    error = adError?.toAdError(),
)

internal fun GmaAdValue.toCommon(): AdValue = AdValue(valueMicros, currencyCode, precisionType.toCommon())

internal fun PrecisionType.toCommon(): AdValuePrecision = when (this) {
    PrecisionType.ESTIMATED -> AdValuePrecision.ESTIMATED
    PrecisionType.PUBLISHER_PROVIDED -> AdValuePrecision.PUBLISHER_PROVIDED
    PrecisionType.PRECISE -> AdValuePrecision.PRECISE
    else -> AdValuePrecision.UNKNOWN
}

internal fun consentStatusOf(status: Int): ConsentStatus = when (status) {
    ConsentInformation.ConsentStatus.REQUIRED -> ConsentStatus.REQUIRED
    ConsentInformation.ConsentStatus.NOT_REQUIRED -> ConsentStatus.NOT_REQUIRED
    ConsentInformation.ConsentStatus.OBTAINED -> ConsentStatus.OBTAINED
    else -> ConsentStatus.UNKNOWN
}

internal fun ConsentInformation.PrivacyOptionsRequirementStatus.toCommon(): PrivacyOptionsRequirementStatus =
    when (this) {
        ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED -> PrivacyOptionsRequirementStatus.REQUIRED
        ConsentInformation.PrivacyOptionsRequirementStatus.NOT_REQUIRED -> PrivacyOptionsRequirementStatus.NOT_REQUIRED
        else -> PrivacyOptionsRequirementStatus.UNKNOWN
    }

internal fun ConsentRequestParameters.toAndroid(context: Context): UmpConsentRequestParameters =
    UmpConsentRequestParameters.Builder()
        .setTagForUnderAgeOfConsent(tagForUnderAgeOfConsent)
        .apply { consentSyncId?.let { setConsentSyncId(it) } }
        .apply { debugSettings?.let { setConsentDebugSettings(it.toAndroid(context)) } }
        .build()

private fun ConsentDebugSettings.toAndroid(context: Context): UmpConsentDebugSettings =
    UmpConsentDebugSettings.Builder(context)
        .setDebugGeography(geography.toAndroid())
        .apply { testDeviceHashedIds.forEach { addTestDeviceHashedId(it) } }
        .build()

internal fun DebugGeography.toAndroid(): Int = when (this) {
    DebugGeography.DISABLED -> UmpConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_DISABLED
    DebugGeography.EEA -> UmpConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA
    DebugGeography.REGULATED_US_STATE -> UmpConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_REGULATED_US_STATE
    DebugGeography.OTHER -> UmpConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_OTHER
}

internal fun MediaAspectRatio.toAndroid(): GmaNativeAd.NativeMediaAspectRatio = when (this) {
    MediaAspectRatio.ANY -> GmaNativeAd.NativeMediaAspectRatio.ANY
    MediaAspectRatio.LANDSCAPE -> GmaNativeAd.NativeMediaAspectRatio.LANDSCAPE
    MediaAspectRatio.PORTRAIT -> GmaNativeAd.NativeMediaAspectRatio.PORTRAIT
    MediaAspectRatio.SQUARE -> GmaNativeAd.NativeMediaAspectRatio.SQUARE
}

internal fun AdChoicesPlacement.toAndroid(): GmaAdChoicesPlacement = when (this) {
    AdChoicesPlacement.TOP_LEFT -> GmaAdChoicesPlacement.TOP_LEFT
    AdChoicesPlacement.TOP_RIGHT -> GmaAdChoicesPlacement.TOP_RIGHT
    AdChoicesPlacement.BOTTOM_RIGHT -> GmaAdChoicesPlacement.BOTTOM_RIGHT
    AdChoicesPlacement.BOTTOM_LEFT -> GmaAdChoicesPlacement.BOTTOM_LEFT
}

internal fun Image.toCommon(): NativeAdImage = NativeAdImage(uri = uri.toString(), scale = scale)

internal fun MediaContent.toCommon(): NativeAdMediaContent = NativeAdMediaContent(
    aspectRatio = aspectRatio.toDouble(),
    hasVideoContent = hasVideoContent,
    durationSeconds = duration.toDouble(),
)
