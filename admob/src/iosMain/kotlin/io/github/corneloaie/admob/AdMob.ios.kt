package io.github.corneloaie.admob

import swiftPMImport.io.github.corneloaie.admob.GADGetStringFromVersionNumber
import swiftPMImport.io.github.corneloaie.admob.GADMobileAds

internal actual fun platformSdkVersion(): String =
    GADGetStringFromVersionNumber(GADMobileAds.sharedInstance.versionNumber)

internal actual fun platformRequestConfiguration(): RequestConfiguration =
    GADMobileAds.sharedInstance.requestConfiguration.toCommon()

internal actual fun setPlatformRequestConfiguration(configuration: RequestConfiguration) {
    GADMobileAds.sharedInstance.requestConfiguration.update(configuration)
}

internal actual fun startPlatformSdk(onComplete: (InitializationStatus) -> Unit) {
    onMainThread {
        GADMobileAds.sharedInstance.startWithCompletionHandler { status ->
            onComplete(status?.toCommon() ?: InitializationStatus(emptyMap()))
        }
    }
}

internal actual fun openPlatformAdInspector(host: AdHost, onClosed: (AdError?) -> Unit) {
    GADMobileAds.sharedInstance.presentAdInspectorFromViewController(host) { error ->
        onClosed(error?.toAdError(AdErrorSource.INSPECTOR))
    }
}
