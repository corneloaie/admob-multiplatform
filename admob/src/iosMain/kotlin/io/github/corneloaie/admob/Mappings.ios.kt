package io.github.corneloaie.admob

import platform.Foundation.NSDecimalNumber
import platform.Foundation.NSError
import platform.Foundation.NSNumber
import swiftPMImport.io.github.corneloaie.admob.GADAdChoicesPosition
import swiftPMImport.io.github.corneloaie.admob.GADAdLoaderOptions
import swiftPMImport.io.github.corneloaie.admob.GADAdNetworkResponseInfo
import swiftPMImport.io.github.corneloaie.admob.GADAdReward
import swiftPMImport.io.github.corneloaie.admob.GADAdValue
import swiftPMImport.io.github.corneloaie.admob.GADAdValuePrecisionEstimated
import swiftPMImport.io.github.corneloaie.admob.GADAdValuePrecisionPrecise
import swiftPMImport.io.github.corneloaie.admob.GADAdValuePrecisionPublisherProvided
import swiftPMImport.io.github.corneloaie.admob.GADAdapterInitializationStateReady
import swiftPMImport.io.github.corneloaie.admob.GADAdapterStatus
import swiftPMImport.io.github.corneloaie.admob.GADErrorAdAlreadyUsed
import swiftPMImport.io.github.corneloaie.admob.GADErrorApplicationIdentifierMissing
import swiftPMImport.io.github.corneloaie.admob.GADErrorInternalError
import swiftPMImport.io.github.corneloaie.admob.GADErrorInvalidArgument
import swiftPMImport.io.github.corneloaie.admob.GADErrorInvalidRequest
import swiftPMImport.io.github.corneloaie.admob.GADErrorMediationAdapterError
import swiftPMImport.io.github.corneloaie.admob.GADErrorMediationDataError
import swiftPMImport.io.github.corneloaie.admob.GADErrorMediationInvalidAdSize
import swiftPMImport.io.github.corneloaie.admob.GADErrorNetworkError
import swiftPMImport.io.github.corneloaie.admob.GADErrorNoFill
import swiftPMImport.io.github.corneloaie.admob.GADErrorReceivedInvalidAdString
import swiftPMImport.io.github.corneloaie.admob.GADErrorServerError
import swiftPMImport.io.github.corneloaie.admob.GADErrorTimeout
import swiftPMImport.io.github.corneloaie.admob.GADErrorUserInfoKeyResponseInfo
import swiftPMImport.io.github.corneloaie.admob.GADInitializationStatus
import swiftPMImport.io.github.corneloaie.admob.GADMaxAdContentRatingGeneral
import swiftPMImport.io.github.corneloaie.admob.GADMaxAdContentRatingMatureAudience
import swiftPMImport.io.github.corneloaie.admob.GADMaxAdContentRatingParentalGuidance
import swiftPMImport.io.github.corneloaie.admob.GADMaxAdContentRatingTeen
import swiftPMImport.io.github.corneloaie.admob.GADMediaAspectRatioAny
import swiftPMImport.io.github.corneloaie.admob.GADMediaAspectRatioLandscape
import swiftPMImport.io.github.corneloaie.admob.GADMediaAspectRatioPortrait
import swiftPMImport.io.github.corneloaie.admob.GADMediaAspectRatioSquare
import swiftPMImport.io.github.corneloaie.admob.GADMediaContent
import swiftPMImport.io.github.corneloaie.admob.GADNativeAdImage
import swiftPMImport.io.github.corneloaie.admob.GADNativeAdMediaAdLoaderOptions
import swiftPMImport.io.github.corneloaie.admob.GADNativeAdViewAdOptions
import swiftPMImport.io.github.corneloaie.admob.GADPresentationErrorCodeAdAlreadyUsed
import swiftPMImport.io.github.corneloaie.admob.GADPresentationErrorCodeAdNotReady
import swiftPMImport.io.github.corneloaie.admob.GADPresentationErrorCodeInternal
import swiftPMImport.io.github.corneloaie.admob.GADPresentationErrorMediation
import swiftPMImport.io.github.corneloaie.admob.GADPresentationErrorNotMainThread
import swiftPMImport.io.github.corneloaie.admob.GADRequest
import swiftPMImport.io.github.corneloaie.admob.GADRequestConfiguration
import swiftPMImport.io.github.corneloaie.admob.GADResponseInfo
import swiftPMImport.io.github.corneloaie.admob.GADVideoOptions
import swiftPMImport.io.github.corneloaie.admob.UMPConsentStatusNotRequired
import swiftPMImport.io.github.corneloaie.admob.UMPConsentStatusObtained
import swiftPMImport.io.github.corneloaie.admob.UMPConsentStatusRequired
import swiftPMImport.io.github.corneloaie.admob.UMPDebugGeographyDisabled
import swiftPMImport.io.github.corneloaie.admob.UMPDebugGeographyEEA
import swiftPMImport.io.github.corneloaie.admob.UMPDebugGeographyOther
import swiftPMImport.io.github.corneloaie.admob.UMPDebugGeographyRegulatedUSState
import swiftPMImport.io.github.corneloaie.admob.UMPDebugSettings
import swiftPMImport.io.github.corneloaie.admob.UMPFormErrorCodeAlreadyUsed
import swiftPMImport.io.github.corneloaie.admob.UMPFormErrorCodeInternal
import swiftPMImport.io.github.corneloaie.admob.UMPFormErrorCodeInvalidViewController
import swiftPMImport.io.github.corneloaie.admob.UMPFormErrorCodeTimeout
import swiftPMImport.io.github.corneloaie.admob.UMPFormErrorCodeUnavailable
import swiftPMImport.io.github.corneloaie.admob.UMPPrivacyOptionsRequirementStatusNotRequired
import swiftPMImport.io.github.corneloaie.admob.UMPPrivacyOptionsRequirementStatusRequired
import swiftPMImport.io.github.corneloaie.admob.UMPRequestErrorCodeInternal
import swiftPMImport.io.github.corneloaie.admob.UMPRequestErrorCodeInvalidAppID
import swiftPMImport.io.github.corneloaie.admob.UMPRequestErrorCodeMisconfiguration
import swiftPMImport.io.github.corneloaie.admob.UMPRequestErrorCodeNetwork
import swiftPMImport.io.github.corneloaie.admob.UMPRequestParameters

// Every SDK value is mapped by its SDK constant, never by its number: Android and iOS number some of them differently.

// The child-directed and under-age tags are deprecated in favor of ageRestrictedTreatment, but still apply.
@Suppress("DEPRECATION")
internal fun GADRequestConfiguration.toCommon(): RequestConfiguration = RequestConfiguration(
    testDeviceIds = testDeviceIdentifiers.orEmpty().filterIsInstance<String>(),
    tagForChildDirectedTreatment = tagForChildDirectedTreatment?.boolValue,
    tagForUnderAgeOfConsent = tagForUnderAgeOfConsent?.boolValue,
    maxAdContentRating = when (maxAdContentRating) {
        GADMaxAdContentRatingGeneral -> MaxAdContentRating.G
        GADMaxAdContentRatingParentalGuidance -> MaxAdContentRating.PG
        GADMaxAdContentRatingTeen -> MaxAdContentRating.T
        GADMaxAdContentRatingMatureAudience -> MaxAdContentRating.MA
        else -> null
    },
)

@Suppress("DEPRECATION")
internal fun GADRequestConfiguration.update(configuration: RequestConfiguration) {
    testDeviceIdentifiers = configuration.testDeviceIds
    tagForChildDirectedTreatment = configuration.tagForChildDirectedTreatment?.let { NSNumber(bool = it) }
    tagForUnderAgeOfConsent = configuration.tagForUnderAgeOfConsent?.let { NSNumber(bool = it) }
    maxAdContentRating = when (configuration.maxAdContentRating) {
        MaxAdContentRating.G -> GADMaxAdContentRatingGeneral
        MaxAdContentRating.PG -> GADMaxAdContentRatingParentalGuidance
        MaxAdContentRating.T -> GADMaxAdContentRatingTeen
        MaxAdContentRating.MA -> GADMaxAdContentRatingMatureAudience
        null -> null
    }
}

internal fun GADInitializationStatus.toCommon(): InitializationStatus = InitializationStatus(
    adapterStatusesByClassName.entries
        .mapNotNull { (name, status) ->
            if (name is String && status is GADAdapterStatus) name to status.toCommon() else null
        }
        .toMap(),
)

private fun GADAdapterStatus.toCommon() = AdapterStatus(
    state = if (state == GADAdapterInitializationStateReady) AdapterState.READY else AdapterState.NOT_READY,
    description = description.orEmpty(),
    latencyMillis = (latency * MILLIS_PER_SECOND).toLong(),
)

internal fun AdRequest.toIos(): GADRequest = GADRequest().also { request ->
    request.keywords = keywords.toList().ifEmpty { null }
    request.contentURL = contentUrl
    request.neighboringContentURLStrings = neighboringContentUrls.ifEmpty { null }
}

internal fun NSError.toAdError(source: AdErrorSource): AdError = AdError(
    code = adErrorCodeOf(source, code),
    platformCode = code.toInt(),
    domain = domain.orEmpty(),
    message = localizedDescription,
    responseInfo = (userInfo[GADErrorUserInfoKeyResponseInfo] as? GADResponseInfo)?.toCommon(),
)

/**
 * The iOS SDKs' error codes, by the call that reported them: loading and presenting share GADErrorDomain, and 21 is an
 * invalid ad string to one and the wrong thread to the other.
 */
internal fun adErrorCodeOf(source: AdErrorSource, code: Long): AdErrorCode = when (source) {
    AdErrorSource.LOAD -> when (code) {
        GADErrorNoFill -> AdErrorCode.NO_FILL
        GADErrorNetworkError -> AdErrorCode.NETWORK_ERROR
        GADErrorTimeout -> AdErrorCode.TIMEOUT
        GADErrorInvalidRequest, GADErrorInvalidArgument, GADErrorReceivedInvalidAdString -> AdErrorCode.INVALID_REQUEST
        GADErrorApplicationIdentifierMissing -> AdErrorCode.APP_ID_MISSING
        GADErrorMediationDataError, GADErrorMediationAdapterError, GADErrorMediationInvalidAdSize ->
            AdErrorCode.MEDIATION_ERROR
        GADErrorAdAlreadyUsed -> AdErrorCode.ALREADY_USED
        GADErrorServerError, GADErrorInternalError -> AdErrorCode.INTERNAL_ERROR
        else -> AdErrorCode.UNKNOWN
    }
    AdErrorSource.SHOW -> when (code) {
        GADPresentationErrorCodeAdNotReady -> AdErrorCode.NOT_READY
        GADPresentationErrorCodeAdAlreadyUsed, GADErrorAdAlreadyUsed -> AdErrorCode.ALREADY_USED
        GADPresentationErrorMediation -> AdErrorCode.MEDIATION_ERROR
        GADPresentationErrorNotMainThread -> AdErrorCode.INVALID_REQUEST
        GADPresentationErrorCodeInternal -> AdErrorCode.INTERNAL_ERROR
        else -> AdErrorCode.UNKNOWN
    }
    AdErrorSource.CONSENT -> when (code) {
        UMPRequestErrorCodeNetwork -> AdErrorCode.NETWORK_ERROR
        UMPFormErrorCodeTimeout -> AdErrorCode.TIMEOUT
        UMPRequestErrorCodeInvalidAppID -> AdErrorCode.APP_ID_MISSING
        UMPRequestErrorCodeMisconfiguration -> AdErrorCode.INVALID_REQUEST
        UMPFormErrorCodeAlreadyUsed -> AdErrorCode.ALREADY_USED
        UMPFormErrorCodeUnavailable, UMPFormErrorCodeInvalidViewController -> AdErrorCode.NOT_READY
        UMPRequestErrorCodeInternal, UMPFormErrorCodeInternal -> AdErrorCode.INTERNAL_ERROR
        else -> AdErrorCode.UNKNOWN
    }
    // The iOS SDK documents no Ad Inspector error codes.
    AdErrorSource.INSPECTOR -> AdErrorCode.UNKNOWN
}

internal fun GADResponseInfo.toCommon(): ResponseInfo = ResponseInfo(
    responseId = responseIdentifier,
    mediationAdapterClassName = loadedAdNetworkResponseInfo?.adNetworkClassName,
    loadedAdapterResponseInfo = loadedAdNetworkResponseInfo?.toCommon(),
    adapterResponses = adNetworkInfoArray.mapNotNull { (it as? GADAdNetworkResponseInfo)?.toCommon() },
)

private fun GADAdNetworkResponseInfo.toCommon() = AdapterResponseInfo(
    adapterClassName = adNetworkClassName,
    adSourceName = adSourceName.orNullIfEmpty(),
    adSourceId = adSourceID.orNullIfEmpty(),
    adSourceInstanceName = adSourceInstanceName.orNullIfEmpty(),
    adSourceInstanceId = adSourceInstanceID.orNullIfEmpty(),
    latencyMillis = (latency * MILLIS_PER_SECOND).toLong(),
    error = error?.toAdError(AdErrorSource.LOAD),
)

internal fun GADAdValue.toCommon(): AdValue = AdValue(
    valueMicros = value.decimalNumberByMultiplyingBy(NSDecimalNumber(longLong = MICROS_PER_UNIT)).longLongValue,
    currencyCode = currencyCode,
    precision = adValuePrecisionOf(precision),
)

internal fun adValuePrecisionOf(precision: Long): AdValuePrecision = when (precision) {
    GADAdValuePrecisionEstimated -> AdValuePrecision.ESTIMATED
    GADAdValuePrecisionPublisherProvided -> AdValuePrecision.PUBLISHER_PROVIDED
    GADAdValuePrecisionPrecise -> AdValuePrecision.PRECISE
    else -> AdValuePrecision.UNKNOWN
}

internal fun consentStatusOf(status: Long): ConsentStatus = when (status) {
    UMPConsentStatusRequired -> ConsentStatus.REQUIRED
    UMPConsentStatusNotRequired -> ConsentStatus.NOT_REQUIRED
    UMPConsentStatusObtained -> ConsentStatus.OBTAINED
    else -> ConsentStatus.UNKNOWN
}

internal fun privacyOptionsRequirementStatusOf(status: Long): PrivacyOptionsRequirementStatus = when (status) {
    UMPPrivacyOptionsRequirementStatusRequired -> PrivacyOptionsRequirementStatus.REQUIRED
    UMPPrivacyOptionsRequirementStatusNotRequired -> PrivacyOptionsRequirementStatus.NOT_REQUIRED
    else -> PrivacyOptionsRequirementStatus.UNKNOWN
}

internal fun ConsentRequestParameters.toIos(): UMPRequestParameters = UMPRequestParameters().also { parameters ->
    parameters.tagForUnderAgeOfConsent = tagForUnderAgeOfConsent
    parameters.debugSettings = debugSettings?.toIos()
}

private fun ConsentDebugSettings.toIos() = UMPDebugSettings().also { settings ->
    settings.geography = geography.toIos()
    settings.testDeviceIdentifiers = testDeviceHashedIds
}

internal fun DebugGeography.toIos(): Long = when (this) {
    DebugGeography.DISABLED -> UMPDebugGeographyDisabled
    DebugGeography.EEA -> UMPDebugGeographyEEA
    DebugGeography.REGULATED_US_STATE -> UMPDebugGeographyRegulatedUSState
    DebugGeography.OTHER -> UMPDebugGeographyOther
}

internal fun NativeAdOptions.toIos(): List<GADAdLoaderOptions> = buildList {
    mediaAspectRatio?.let { add(GADNativeAdMediaAdLoaderOptions().apply { mediaAspectRatio = it.toIos() }) }
    adChoicesPlacement?.let { add(GADNativeAdViewAdOptions().apply { preferredAdChoicesPosition = it.toIos() }) }
    startMuted?.let { add(GADVideoOptions().apply { startMuted = it }) }
}

internal fun MediaAspectRatio.toIos(): Long = when (this) {
    MediaAspectRatio.ANY -> GADMediaAspectRatioAny
    MediaAspectRatio.LANDSCAPE -> GADMediaAspectRatioLandscape
    MediaAspectRatio.PORTRAIT -> GADMediaAspectRatioPortrait
    MediaAspectRatio.SQUARE -> GADMediaAspectRatioSquare
}

internal fun AdChoicesPlacement.toIos(): GADAdChoicesPosition = when (this) {
    AdChoicesPlacement.TOP_LEFT -> GADAdChoicesPosition.GADAdChoicesPositionTopLeftCorner
    AdChoicesPlacement.TOP_RIGHT -> GADAdChoicesPosition.GADAdChoicesPositionTopRightCorner
    AdChoicesPlacement.BOTTOM_RIGHT -> GADAdChoicesPosition.GADAdChoicesPositionBottomRightCorner
    AdChoicesPlacement.BOTTOM_LEFT -> GADAdChoicesPosition.GADAdChoicesPositionBottomLeftCorner
}

internal fun GADNativeAdImage.toCommon(): NativeAdImage = NativeAdImage(uri = imageURL?.absoluteString, scale = scale)

internal fun GADMediaContent.toCommon(): NativeAdMediaContent = NativeAdMediaContent(
    aspectRatio = aspectRatio,
    hasVideoContent = hasVideoContent,
    durationSeconds = duration,
)

internal fun GADAdReward.toCommon(): RewardItem = RewardItem(type = type, amount = amount.intValue)

private const val MILLIS_PER_SECOND = 1000
private const val MICROS_PER_UNIT = 1_000_000L
