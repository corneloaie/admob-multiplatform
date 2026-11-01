package io.github.corneloaie.admob

import swiftPMImport.io.github.corneloaie.admob.UMPConsentForm
import swiftPMImport.io.github.corneloaie.admob.UMPConsentInformation
import swiftPMImport.io.github.corneloaie.admob.UMPFormStatusAvailable

internal actual fun consentPlatform(): ConsentPlatform<AdHost> = IosConsentPlatform

private object IosConsentPlatform : ConsentPlatform<AdHost> {
    private val info get() = UMPConsentInformation.sharedInstance

    override val consentStatus get() = consentStatusOf(info.consentStatus)
    override val canRequestAds get() = info.canRequestAds
    override val privacyOptionsRequirementStatus
        get() = privacyOptionsRequirementStatusOf(info.privacyOptionsRequirementStatus)
    override val isConsentFormAvailable get() = info.formStatus == UMPFormStatusAvailable

    // UMP on iOS needs no view controller to update.
    override fun requestInfoUpdate(host: AdHost, parameters: ConsentRequestParameters, onComplete: (AdError?) -> Unit) {
        info.requestConsentInfoUpdateWithParameters(parameters.toIos()) { error ->
            onComplete(error?.toAdError(AdErrorSource.CONSENT))
        }
    }

    override fun loadAndShowConsentFormIfRequired(host: AdHost, onComplete: (AdError?) -> Unit) {
        UMPConsentForm.loadAndPresentIfRequiredFromViewController(host) { error ->
            onComplete(error?.toAdError(AdErrorSource.CONSENT))
        }
    }

    override fun showPrivacyOptionsForm(host: AdHost, onComplete: (AdError?) -> Unit) {
        UMPConsentForm.presentPrivacyOptionsFormFromViewController(host) { error ->
            onComplete(error?.toAdError(AdErrorSource.CONSENT))
        }
    }

    override fun reset() {
        info.reset()
    }
}
