package io.github.corneloaie.admob

import android.content.Context
import com.google.android.gms.ads.AdInspectorError
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MediaContent
import com.google.android.gms.ads.VideoOptions
import com.google.android.ump.ConsentInformation
import com.google.android.ump.FormError
import com.google.android.gms.ads.AdError as GmaAdError
import com.google.android.gms.ads.AdRequest as GmaAdRequest
import com.google.android.gms.ads.AdValue as GmaAdValue
import com.google.android.gms.ads.AdapterResponseInfo as GmaAdapterResponseInfo
import com.google.android.gms.ads.RequestConfiguration as GmaRequestConfiguration
import com.google.android.gms.ads.ResponseInfo as GmaResponseInfo
import com.google.android.gms.ads.initialization.AdapterStatus as GmaAdapterStatus
import com.google.android.gms.ads.initialization.InitializationStatus as GmaInitializationStatus
import com.google.android.gms.ads.nativead.NativeAd as GmaNativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions as GmaNativeAdOptions
import com.google.android.ump.ConsentDebugSettings as UmpConsentDebugSettings
import com.google.android.ump.ConsentRequestParameters as UmpConsentRequestParameters

// Every SDK value is mapped by its SDK constant, never by its number: Android and iOS number some of them differently.

internal fun RequestConfiguration.toAndroid(builder: GmaRequestConfiguration.Builder): GmaRequestConfiguration = builder
    .setTestDeviceIds(testDeviceIds)
    .setTagForChildDirectedTreatment(
        when (tagForChildDirectedTreatment) {
            true -> GmaRequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE
            false -> GmaRequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_FALSE
            null -> GmaRequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED
        },
    )
    .setTagForUnderAgeOfConsent(
        when (tagForUnderAgeOfConsent) {
            true -> GmaRequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_TRUE
            false -> GmaRequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_FALSE
            null -> GmaRequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_UNSPECIFIED
        },
    )
    .setMaxAdContentRating(
        when (maxAdContentRating) {
            MaxAdContentRating.G -> GmaRequestConfiguration.MAX_AD_CONTENT_RATING_G
            MaxAdContentRating.PG -> GmaRequestConfiguration.MAX_AD_CONTENT_RATING_PG
            MaxAdContentRating.T -> GmaRequestConfiguration.MAX_AD_CONTENT_RATING_T
            MaxAdContentRating.MA -> GmaRequestConfiguration.MAX_AD_CONTENT_RATING_MA
            null -> GmaRequestConfiguration.MAX_AD_CONTENT_RATING_UNSPECIFIED
        },
    )
    .build()

internal fun GmaRequestConfiguration.toCommon(): RequestConfiguration = RequestConfiguration(
    testDeviceIds = testDeviceIds.toList(),
    tagForChildDirectedTreatment = when (tagForChildDirectedTreatment) {
        GmaRequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE -> true
        GmaRequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_FALSE -> false
        else -> null
    },
    tagForUnderAgeOfConsent = when (tagForUnderAgeOfConsent) {
        GmaRequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_TRUE -> true
        GmaRequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_FALSE -> false
        else -> null
    },
    maxAdContentRating = when (maxAdContentRating) {
        GmaRequestConfiguration.MAX_AD_CONTENT_RATING_G -> MaxAdContentRating.G
        GmaRequestConfiguration.MAX_AD_CONTENT_RATING_PG -> MaxAdContentRating.PG
        GmaRequestConfiguration.MAX_AD_CONTENT_RATING_T -> MaxAdContentRating.T
        GmaRequestConfiguration.MAX_AD_CONTENT_RATING_MA -> MaxAdContentRating.MA
        else -> null
    },
)

internal fun GmaInitializationStatus.toCommon(): InitializationStatus =
    InitializationStatus(adapterStatusMap.mapValues { (_, status) -> status.toCommon() })

private fun GmaAdapterStatus.toCommon() = AdapterStatus(
    state = if (initializationState == GmaAdapterStatus.State.READY) AdapterState.READY else AdapterState.NOT_READY,
    description = description,
    latencyMillis = latency.toLong(),
)

internal fun AdRequest.toAndroid(): GmaAdRequest = GmaAdRequest.Builder()
    .apply {
        keywords.forEach { addKeyword(it) }
        contentUrl?.let { setContentUrl(it) }
        if (neighboringContentUrls.isNotEmpty()) setNeighboringContentUrls(neighboringContentUrls)
    }
    .build()

internal fun LoadAdError.toAdError(): AdError = toAdError(AdErrorSource.LOAD, responseInfo?.toCommon())

internal fun GmaAdError.toAdError(source: AdErrorSource, responseInfo: ResponseInfo? = null): AdError = AdError(
    code = adErrorCodeOf(source, code),
    platformCode = code,
    domain = domain,
    message = message,
    responseInfo = responseInfo,
)

internal fun FormError.toAdError(): AdError = AdError(
    code = adErrorCodeOf(AdErrorSource.CONSENT, errorCode),
    platformCode = errorCode,
    domain = UMP_ERROR_DOMAIN,
    message = message,
)

// UMP's FormError carries no domain of its own.
private const val UMP_ERROR_DOMAIN = "com.google.android.ump"

/** The Android SDKs' error codes, by the call that reported them: each call numbers its errors from 0 or 1. */
internal fun adErrorCodeOf(source: AdErrorSource, code: Int): AdErrorCode = when (source) {
    AdErrorSource.LOAD -> when (code) {
        GmaAdRequest.ERROR_CODE_NO_FILL, GmaAdRequest.ERROR_CODE_MEDIATION_NO_FILL -> AdErrorCode.NO_FILL
        GmaAdRequest.ERROR_CODE_NETWORK_ERROR -> AdErrorCode.NETWORK_ERROR
        GmaAdRequest.ERROR_CODE_INVALID_REQUEST,
        GmaAdRequest.ERROR_CODE_REQUEST_ID_MISMATCH,
        GmaAdRequest.ERROR_CODE_INVALID_AD_STRING,
        -> AdErrorCode.INVALID_REQUEST
        GmaAdRequest.ERROR_CODE_APP_ID_MISSING -> AdErrorCode.APP_ID_MISSING
        GmaAdRequest.ERROR_CODE_INTERNAL_ERROR -> AdErrorCode.INTERNAL_ERROR
        else -> AdErrorCode.UNKNOWN
    }
    AdErrorSource.SHOW -> when (code) {
        FullScreenContentCallback.ERROR_CODE_AD_REUSED -> AdErrorCode.ALREADY_USED
        FullScreenContentCallback.ERROR_CODE_NOT_READY,
        FullScreenContentCallback.ERROR_CODE_APP_NOT_FOREGROUND,
        -> AdErrorCode.NOT_READY
        FullScreenContentCallback.ERROR_CODE_MEDIATION_SHOW_ERROR -> AdErrorCode.MEDIATION_ERROR
        FullScreenContentCallback.ERROR_CODE_INTERNAL_ERROR -> AdErrorCode.INTERNAL_ERROR
        else -> AdErrorCode.UNKNOWN
    }
    AdErrorSource.CONSENT -> when (code) {
        FormError.ErrorCode.INTERNET_ERROR -> AdErrorCode.NETWORK_ERROR
        FormError.ErrorCode.TIME_OUT -> AdErrorCode.TIMEOUT
        FormError.ErrorCode.INVALID_OPERATION -> AdErrorCode.INVALID_REQUEST
        FormError.ErrorCode.INTERNAL_ERROR -> AdErrorCode.INTERNAL_ERROR
        else -> AdErrorCode.UNKNOWN
    }
    AdErrorSource.INSPECTOR -> when (code) {
        AdInspectorError.ERROR_CODE_NOT_IN_TEST_MODE -> AdErrorCode.NOT_IN_TEST_MODE
        AdInspectorError.ERROR_CODE_ALREADY_OPEN -> AdErrorCode.NOT_READY
        AdInspectorError.ERROR_CODE_FAILED_TO_LOAD -> AdErrorCode.NETWORK_ERROR
        AdInspectorError.ERROR_CODE_INTERNAL_ERROR -> AdErrorCode.INTERNAL_ERROR
        else -> AdErrorCode.UNKNOWN
    }
}

internal fun GmaResponseInfo.toCommon(): ResponseInfo = ResponseInfo(
    responseId = responseId,
    mediationAdapterClassName = mediationAdapterClassName,
    loadedAdapterResponseInfo = loadedAdapterResponseInfo?.toCommon(),
    adapterResponses = adapterResponses.map { it.toCommon() },
)

private fun GmaAdapterResponseInfo.toCommon() = AdapterResponseInfo(
    adapterClassName = adapterClassName,
    adSourceName = adSourceName.orNullIfEmpty(),
    adSourceId = adSourceId.orNullIfEmpty(),
    adSourceInstanceName = adSourceInstanceName.orNullIfEmpty(),
    adSourceInstanceId = adSourceInstanceId.orNullIfEmpty(),
    latencyMillis = latencyMillis,
    error = adError?.toAdError(AdErrorSource.LOAD),
)

internal fun GmaAdValue.toCommon(): AdValue = AdValue(valueMicros, currencyCode, adValuePrecisionOf(precisionType))

internal fun adValuePrecisionOf(precisionType: Int): AdValuePrecision = when (precisionType) {
    GmaAdValue.PrecisionType.ESTIMATED -> AdValuePrecision.ESTIMATED
    GmaAdValue.PrecisionType.PUBLISHER_PROVIDED -> AdValuePrecision.PUBLISHER_PROVIDED
    GmaAdValue.PrecisionType.PRECISE -> AdValuePrecision.PRECISE
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

internal fun NativeAdOptions.toAndroid(): GmaNativeAdOptions = GmaNativeAdOptions.Builder()
    .apply {
        mediaAspectRatio?.let { setMediaAspectRatio(it.toAndroid()) }
        adChoicesPlacement?.let { setAdChoicesPlacement(it.toAndroid()) }
        startMuted?.let { setVideoOptions(VideoOptions.Builder().setStartMuted(it).build()) }
    }
    .build()

internal fun MediaAspectRatio.toAndroid(): Int = when (this) {
    MediaAspectRatio.ANY -> GmaNativeAdOptions.NATIVE_MEDIA_ASPECT_RATIO_ANY
    MediaAspectRatio.LANDSCAPE -> GmaNativeAdOptions.NATIVE_MEDIA_ASPECT_RATIO_LANDSCAPE
    MediaAspectRatio.PORTRAIT -> GmaNativeAdOptions.NATIVE_MEDIA_ASPECT_RATIO_PORTRAIT
    MediaAspectRatio.SQUARE -> GmaNativeAdOptions.NATIVE_MEDIA_ASPECT_RATIO_SQUARE
}

internal fun AdChoicesPlacement.toAndroid(): Int = when (this) {
    AdChoicesPlacement.TOP_LEFT -> GmaNativeAdOptions.ADCHOICES_TOP_LEFT
    AdChoicesPlacement.TOP_RIGHT -> GmaNativeAdOptions.ADCHOICES_TOP_RIGHT
    AdChoicesPlacement.BOTTOM_RIGHT -> GmaNativeAdOptions.ADCHOICES_BOTTOM_RIGHT
    AdChoicesPlacement.BOTTOM_LEFT -> GmaNativeAdOptions.ADCHOICES_BOTTOM_LEFT
}

internal fun GmaNativeAd.Image.toCommon(): NativeAdImage = NativeAdImage(uri = uri?.toString(), scale = scale)

internal fun MediaContent.toCommon(): NativeAdMediaContent = NativeAdMediaContent(
    aspectRatio = aspectRatio.toDouble(),
    hasVideoContent = hasVideoContent(),
    durationSeconds = duration.toDouble(),
)
