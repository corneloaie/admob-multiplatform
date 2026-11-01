package io.github.corneloaie.admob

import com.google.android.ump.ConsentInformation
import com.google.android.ump.UserMessagingPlatform

internal actual fun consentPlatform(): ConsentPlatform<AdHost> =
    AndroidConsentPlatform(UserMessagingPlatform.getConsentInformation(applicationContext()))

private class AndroidConsentPlatform(private val info: ConsentInformation) : ConsentPlatform<AdHost> {
    override val consentStatus get() = consentStatusOf(info.consentStatus)
    override val canRequestAds get() = info.canRequestAds()
    override val privacyOptionsRequirementStatus get() = info.privacyOptionsRequirementStatus.toCommon()
    override val isConsentFormAvailable get() = info.isConsentFormAvailable

    override fun requestInfoUpdate(host: AdHost, parameters: ConsentRequestParameters, onComplete: (AdError?) -> Unit) {
        info.requestConsentInfoUpdate(
            host,
            parameters.toAndroid(host),
            { onComplete(null) },
            { error -> onComplete(error.toAdError()) },
        )
    }

    override fun loadAndShowConsentFormIfRequired(host: AdHost, onComplete: (AdError?) -> Unit) {
        UserMessagingPlatform.loadAndShowConsentFormIfRequired(host) { error -> onComplete(error?.toAdError()) }
    }

    override fun showPrivacyOptionsForm(host: AdHost, onComplete: (AdError?) -> Unit) {
        UserMessagingPlatform.showPrivacyOptionsForm(host) { error -> onComplete(error?.toAdError()) }
    }

    override fun reset() {
        info.reset()
    }
}
