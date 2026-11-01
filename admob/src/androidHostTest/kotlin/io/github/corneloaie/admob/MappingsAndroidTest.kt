package io.github.corneloaie.admob

import com.google.android.gms.ads.AdValue
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentInformation
import com.google.android.ump.FormError
import kotlin.test.Test
import kotlin.test.assertEquals
import com.google.android.gms.ads.AdRequest as GmaAdRequest
import com.google.android.gms.ads.RequestConfiguration as GmaRequestConfiguration

/** The Android numbering, which differs from iOS's: these would break if a mapping went by number. */
class MappingsAndroidTest {
    @Test
    fun consentStatusFollowsTheAndroidNumbering() {
        // NotRequired is 1 and Required 2 on Android: the other way round on iOS.
        assertEquals(2, ConsentInformation.ConsentStatus.REQUIRED)
        assertEquals(ConsentStatus.REQUIRED, consentStatusOf(ConsentInformation.ConsentStatus.REQUIRED))
        assertEquals(ConsentStatus.NOT_REQUIRED, consentStatusOf(ConsentInformation.ConsentStatus.NOT_REQUIRED))
        assertEquals(ConsentStatus.OBTAINED, consentStatusOf(ConsentInformation.ConsentStatus.OBTAINED))
        assertEquals(ConsentStatus.UNKNOWN, consentStatusOf(ConsentInformation.ConsentStatus.UNKNOWN))
        assertEquals(
            PrivacyOptionsRequirementStatus.REQUIRED,
            ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED.toCommon(),
        )
    }

    @Test
    fun errorCodesFollowTheCallThatReportedThem() {
        assertEquals(3, GmaAdRequest.ERROR_CODE_NO_FILL)
        assertEquals(AdErrorCode.NO_FILL, adErrorCodeOf(AdErrorSource.LOAD, GmaAdRequest.ERROR_CODE_NO_FILL))
        assertEquals(AdErrorCode.NO_FILL, adErrorCodeOf(AdErrorSource.LOAD, GmaAdRequest.ERROR_CODE_MEDIATION_NO_FILL))
        // 1 is an invalid request to a load and a reused ad to a show.
        assertEquals(AdErrorCode.INVALID_REQUEST, adErrorCodeOf(AdErrorSource.LOAD, 1))
        assertEquals(AdErrorCode.ALREADY_USED, adErrorCodeOf(AdErrorSource.SHOW, FullScreenContentCallback.ERROR_CODE_AD_REUSED))
        assertEquals(AdErrorCode.NETWORK_ERROR, adErrorCodeOf(AdErrorSource.CONSENT, FormError.ErrorCode.INTERNET_ERROR))
        assertEquals(AdErrorCode.UNKNOWN, adErrorCodeOf(AdErrorSource.LOAD, 999))
    }

    @Test
    fun aLoadErrorKeepsTheSdkCodeAndDomain() {
        val error = LoadAdError(GmaAdRequest.ERROR_CODE_NO_FILL, "No fill.", "com.google.android.gms.ads", null, null)

        assertEquals(
            AdError(AdErrorCode.NO_FILL, platformCode = 3, domain = "com.google.android.gms.ads", message = "No fill."),
            error.toAdError(),
        )
    }

    @Test
    fun aFormErrorBecomesAConsentError() {
        assertEquals(
            AdError(AdErrorCode.TIMEOUT, platformCode = 4, domain = "com.google.android.ump", message = "slow"),
            FormError(FormError.ErrorCode.TIME_OUT, "slow").toAdError(),
        )
    }

    @Test
    fun requestConfigurationRoundTripsThroughTheSdkType() {
        val configuration = RequestConfiguration(
            testDeviceIds = listOf("TEST-DEVICE"),
            tagForChildDirectedTreatment = true,
            tagForUnderAgeOfConsent = false,
            maxAdContentRating = MaxAdContentRating.T,
        )

        assertEquals(configuration, configuration.toAndroid(GmaRequestConfiguration.Builder()).toCommon())
        assertEquals(RequestConfiguration(), RequestConfiguration().toAndroid(GmaRequestConfiguration.Builder()).toCommon())
    }

    @Test
    fun optionsMapToTheAndroidConstants() {
        // TopLeft is the first AdChoices position on Android, TopRight the first on iOS.
        assertEquals(NativeAdOptions.ADCHOICES_TOP_LEFT, AdChoicesPlacement.TOP_LEFT.toAndroid())
        assertEquals(NativeAdOptions.ADCHOICES_TOP_RIGHT, AdChoicesPlacement.TOP_RIGHT.toAndroid())
        assertEquals(NativeAdOptions.NATIVE_MEDIA_ASPECT_RATIO_LANDSCAPE, MediaAspectRatio.LANDSCAPE.toAndroid())
        assertEquals(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA, DebugGeography.EEA.toAndroid())
        assertEquals(AdValuePrecision.PRECISE, adValuePrecisionOf(AdValue.PrecisionType.PRECISE))
    }
}
