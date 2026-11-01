package io.github.corneloaie.admob.spike

import platform.Foundation.NSError
import platform.UIKit.UIViewController
import swiftPMImport.io.github.corneloaie.admob.GADMobileAds
import swiftPMImport.io.github.corneloaie.admob.UMPConsentForm
import swiftPMImport.io.github.corneloaie.admob.UMPConsentInformation
import swiftPMImport.io.github.corneloaie.admob.UMPRequestParameters

/**
 * Spike: InfoTraffic's Swift `AdsConsentCoordinator` (iOSApp.swift) written against the UMP + GMA SDKs from Kotlin.
 * Every call the Swift class makes has a Kotlin equivalent here.
 */
internal object ConsentProbe {
    /** UMPConsentStatus: 0 unknown, 1 required, 2 not required, 3 obtained. */
    val consentStatus: Long get() = UMPConsentInformation.sharedInstance.consentStatus

    val canRequestAds: Boolean get() = UMPConsentInformation.sharedInstance.canRequestAds

    fun refreshInfo(onDone: (NSError?) -> Unit) {
        val params = UMPRequestParameters().apply { tagForUnderAgeOfConsent = false }
        UMPConsentInformation.sharedInstance.requestConsentInfoUpdateWithParameters(params) { error -> onDone(error) }
    }

    fun resetConsent() {
        UMPConsentInformation.sharedInstance.reset()
    }

    fun showFormIfRequired(from: UIViewController, onDone: (NSError?) -> Unit) {
        UMPConsentForm.loadAndPresentIfRequiredFromViewController(from) { error -> onDone(error) }
    }

    fun showPrivacyOptionsForm(from: UIViewController, onDone: (NSError?) -> Unit) {
        UMPConsentForm.presentPrivacyOptionsFormFromViewController(from) { error -> onDone(error) }
    }

    fun startMobileAds() {
        GADMobileAds.sharedInstance.startWithCompletionHandler(null)
    }

    fun presentAdInspector(from: UIViewController, onDone: (NSError?) -> Unit) {
        GADMobileAds.sharedInstance.presentAdInspectorFromViewController(from) { error -> onDone(error) }
    }
}
