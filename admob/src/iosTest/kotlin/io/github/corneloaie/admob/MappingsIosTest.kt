package io.github.corneloaie.admob

import swiftPMImport.io.github.corneloaie.admob.GADAdChoicesPosition
import swiftPMImport.io.github.corneloaie.admob.GADAdValuePrecisionPrecise
import swiftPMImport.io.github.corneloaie.admob.GADAgeRestrictedTreatmentChild
import swiftPMImport.io.github.corneloaie.admob.GADErrorNoFill
import swiftPMImport.io.github.corneloaie.admob.GADErrorReceivedInvalidAdString
import swiftPMImport.io.github.corneloaie.admob.GADMediaAspectRatioLandscape
import swiftPMImport.io.github.corneloaie.admob.GADPresentationErrorCodeAdAlreadyUsed
import swiftPMImport.io.github.corneloaie.admob.GADPresentationErrorNotMainThread
import swiftPMImport.io.github.corneloaie.admob.GADRequestConfiguration
import swiftPMImport.io.github.corneloaie.admob.UMPConsentStatusNotRequired
import swiftPMImport.io.github.corneloaie.admob.UMPConsentStatusObtained
import swiftPMImport.io.github.corneloaie.admob.UMPConsentStatusRequired
import swiftPMImport.io.github.corneloaie.admob.UMPConsentStatusUnknown
import swiftPMImport.io.github.corneloaie.admob.UMPDebugGeographyEEA
import swiftPMImport.io.github.corneloaie.admob.UMPFormErrorCodeTimeout
import swiftPMImport.io.github.corneloaie.admob.UMPPrivacyOptionsRequirementStatusRequired
import swiftPMImport.io.github.corneloaie.admob.UMPRequestErrorCodeNetwork
import kotlin.test.Test
import kotlin.test.assertEquals

/** The iOS numbering, which differs from Android's: these would break if a mapping went by number. */
class MappingsIosTest {
    @Test
    fun consentStatusFollowsTheIosNumbering() {
        // Required is 1 and NotRequired 2 on iOS: the other way round on Android.
        assertEquals(1L, UMPConsentStatusRequired)
        assertEquals(ConsentStatus.REQUIRED, consentStatusOf(UMPConsentStatusRequired))
        assertEquals(ConsentStatus.NOT_REQUIRED, consentStatusOf(UMPConsentStatusNotRequired))
        assertEquals(ConsentStatus.OBTAINED, consentStatusOf(UMPConsentStatusObtained))
        assertEquals(ConsentStatus.UNKNOWN, consentStatusOf(UMPConsentStatusUnknown))
        assertEquals(
            PrivacyOptionsRequirementStatus.REQUIRED,
            privacyOptionsRequirementStatusOf(UMPPrivacyOptionsRequirementStatusRequired),
        )
    }

    @Test
    fun errorCodesFollowTheCallThatReportedThem() {
        assertEquals(1L, GADErrorNoFill)
        assertEquals(AdErrorCode.NO_FILL, adErrorCodeOf(AdErrorSource.LOAD, GADErrorNoFill))
        // 21 is an invalid ad string to a load and the wrong thread to a presentation.
        assertEquals(GADErrorReceivedInvalidAdString, GADPresentationErrorNotMainThread)
        assertEquals(AdErrorCode.INVALID_REQUEST, adErrorCodeOf(AdErrorSource.LOAD, GADErrorReceivedInvalidAdString))
        assertEquals(AdErrorCode.ALREADY_USED, adErrorCodeOf(AdErrorSource.SHOW, GADPresentationErrorCodeAdAlreadyUsed))
        assertEquals(AdErrorCode.NETWORK_ERROR, adErrorCodeOf(AdErrorSource.CONSENT, UMPRequestErrorCodeNetwork))
        assertEquals(AdErrorCode.TIMEOUT, adErrorCodeOf(AdErrorSource.CONSENT, UMPFormErrorCodeTimeout))
        assertEquals(AdErrorCode.UNKNOWN, adErrorCodeOf(AdErrorSource.LOAD, 999))
        // An adapter's 1 is no GMA no-fill.
        assertEquals(AdErrorCode.MEDIATION_ERROR, adErrorCodeOf(AdErrorSource.ADAPTER, GADErrorNoFill))
    }

    @Test
    fun requestParametersCarryTheNewerSettings() {
        val parameters = ConsentRequestParameters(consentSyncId = "4f0e7c4e-6a3b-4e1b-9a7d-2c5f8e9b1d3a").toIos()
        assertEquals("4f0e7c4e-6a3b-4e1b-9a7d-2c5f8e9b1d3a", parameters.consentSyncID)

        val configuration = GADRequestConfiguration()
        configuration.update(RequestConfiguration(ageRestrictedTreatment = AgeRestrictedTreatment.CHILD))
        assertEquals(GADAgeRestrictedTreatmentChild, configuration.ageRestrictedTreatment)
        assertEquals(AgeRestrictedTreatment.CHILD, configuration.toCommon().ageRestrictedTreatment)
    }

    @Test
    fun optionsMapToTheIosConstants() {
        // TopRight is the first AdChoices position on iOS, TopLeft the first on Android.
        assertEquals(GADAdChoicesPosition.GADAdChoicesPositionTopLeftCorner, AdChoicesPlacement.TOP_LEFT.toIos())
        assertEquals(GADAdChoicesPosition.GADAdChoicesPositionTopRightCorner, AdChoicesPlacement.TOP_RIGHT.toIos())
        assertEquals(GADMediaAspectRatioLandscape, MediaAspectRatio.LANDSCAPE.toIos())
        assertEquals(UMPDebugGeographyEEA, DebugGeography.EEA.toIos())
        assertEquals(AdValuePrecision.PRECISE, adValuePrecisionOf(GADAdValuePrecisionPrecise))
    }
}
