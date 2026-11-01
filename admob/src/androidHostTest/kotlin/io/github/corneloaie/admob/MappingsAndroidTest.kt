package io.github.corneloaie.admob

import com.google.android.libraries.ads.mobile.sdk.common.AdInspectorError
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.common.MediationAdError
import com.google.android.libraries.ads.mobile.sdk.common.PrecisionType
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentInformation
import com.google.android.ump.FormError
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import com.google.android.libraries.ads.mobile.sdk.common.AdChoicesPlacement as GmaAdChoicesPlacement
import com.google.android.libraries.ads.mobile.sdk.common.AgeRestrictedTreatment as GmaAgeRestrictedTreatment
import com.google.android.libraries.ads.mobile.sdk.common.RequestConfiguration as GmaRequestConfiguration
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd as GmaNativeAd

/** The Android SDKs' constants, which differ from iOS's: these would break if a mapping went by number. */
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
        assertEquals(AdErrorCode.NO_FILL, LoadAdError.ErrorCode.NO_FILL.toCommon())
        assertEquals(AdErrorCode.INVALID_REQUEST, LoadAdError.ErrorCode.NOT_FOUND.toCommon())
        assertEquals(AdErrorCode.UNKNOWN, LoadAdError.ErrorCode.CANCELLED.toCommon())
        assertEquals(AdErrorCode.ALREADY_USED, FullScreenContentError.ErrorCode.AD_REUSED.toCommon())
        assertEquals(AdErrorCode.NOT_READY, FullScreenContentError.ErrorCode.APP_NOT_FOREGROUND.toCommon())
        assertEquals(AdErrorCode.NOT_IN_TEST_MODE, AdInspectorError.ErrorCode.NOT_IN_TEST_MODE.toCommon())
        assertEquals(AdErrorCode.NETWORK_ERROR, consentErrorCodeOf(FormError.ErrorCode.INTERNET_ERROR))
        assertEquals(AdErrorCode.UNKNOWN, consentErrorCodeOf(999))
    }

    @Test
    fun aLoadErrorKeepsTheSdkCode() {
        // No fill is 3 on Android and 1 on iOS.
        assertEquals(
            AdError(
                AdErrorCode.NO_FILL,
                platformCode = 3,
                domain = "com.google.android.libraries.ads.mobile.sdk",
                message = "No fill.",
            ),
            LoadAdError(LoadAdError.ErrorCode.NO_FILL, "No fill.", null).toAdError(),
        )
    }

    @Test
    fun aMediationErrorIsTheSdksOrTheAdapters() {
        assertEquals(
            AdErrorCode.NO_FILL,
            MediationAdError(3, "No fill.", MediationAdError.GOOGLE_MOBILE_ADS_DOMAIN).toAdError().code,
        )
        // An adapter numbers its errors itself: its 3 is no GMA no-fill.
        assertEquals(
            AdError(AdErrorCode.MEDIATION_ERROR, platformCode = 3, domain = "com.meta", message = "Bidding failed."),
            MediationAdError(3, "Bidding failed.", "com.meta").toAdError(),
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
    fun requestConfigurationBuildsTheSdkType() {
        val sdk = RequestConfiguration(
            testDeviceIds = listOf("TEST-DEVICE"),
            ageRestrictedTreatment = AgeRestrictedTreatment.TEEN,
            maxAdContentRating = MaxAdContentRating.T,
        ).toAndroid()

        assertTrue("TEST-DEVICE" in sdk.testDeviceIds)
        assertEquals(GmaAgeRestrictedTreatment.TEEN, sdk.ageRestrictedTreatment)
        assertEquals(GmaRequestConfiguration.MaxAdContentRating.MAX_AD_CONTENT_RATING_T, sdk.maxAdContentRating)
        val unset = RequestConfiguration().toAndroid()
        assertEquals(GmaAgeRestrictedTreatment.UNSPECIFIED, unset.ageRestrictedTreatment)
        assertEquals(GmaRequestConfiguration.MaxAdContentRating.MAX_AD_CONTENT_RATING_UNSPECIFIED, unset.maxAdContentRating)
        assertEquals(
            GmaAgeRestrictedTreatment.CHILD,
            RequestConfiguration(ageRestrictedTreatment = AgeRestrictedTreatment.CHILD).toAndroid().ageRestrictedTreatment,
        )
    }

    @Test
    fun optionsMapToTheAndroidConstants() {
        assertEquals(GmaAdChoicesPlacement.TOP_LEFT, AdChoicesPlacement.TOP_LEFT.toAndroid())
        assertEquals(GmaAdChoicesPlacement.TOP_RIGHT, AdChoicesPlacement.TOP_RIGHT.toAndroid())
        assertEquals(GmaNativeAd.NativeMediaAspectRatio.LANDSCAPE, MediaAspectRatio.LANDSCAPE.toAndroid())
        assertEquals(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA, DebugGeography.EEA.toAndroid())
        assertEquals(AdValuePrecision.PRECISE, PrecisionType.PRECISE.toCommon())
    }
}
