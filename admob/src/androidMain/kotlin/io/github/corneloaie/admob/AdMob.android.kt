package io.github.corneloaie.admob

import com.google.android.gms.ads.MobileAds
import kotlin.concurrent.thread

internal actual fun platformSdkVersion(): String = MobileAds.getVersion().toString()

internal actual fun platformRequestConfiguration(): RequestConfiguration = MobileAds.getRequestConfiguration().toCommon()

internal actual fun setPlatformRequestConfiguration(configuration: RequestConfiguration) {
    MobileAds.setRequestConfiguration(configuration.toAndroid(MobileAds.getRequestConfiguration().toBuilder()))
}

internal actual fun startPlatformSdk(onComplete: (InitializationStatus) -> Unit) {
    val context = applicationContext()
    // On a background thread, as Google recommends: initialization reads from disk. The SDK calls back on the main one.
    thread(name = "admob-initialize") {
        MobileAds.initialize(context) { status -> onComplete(status.toCommon()) }
    }
}

internal actual fun openPlatformAdInspector(host: AdHost, onClosed: (AdError?) -> Unit) {
    MobileAds.openAdInspector(host) { error -> onClosed(error?.toAdError(AdErrorSource.INSPECTOR)) }
}
