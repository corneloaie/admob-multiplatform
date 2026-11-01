package io.github.corneloaie.admob

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.google.android.libraries.ads.mobile.sdk.MobileAds
import com.google.android.libraries.ads.mobile.sdk.initialization.InitializationConfig
import kotlinx.coroutines.CompletableDeferred
import kotlin.concurrent.thread

internal actual fun platformSdkVersion(): String = MobileAds.getVersion().toString()

// The Next-Gen SDK takes the request configuration with initialize, and throws when asked for it earlier; read back, it
// has the emulator's test device id added. So the library holds what the app set, and hands it to the SDK once started.
// The lock orders a change against the start.
private val configurationLock = Any()
private var heldConfiguration = RequestConfiguration()
private var sdkStarted = false

// Completes once initialize has returned: the SDK then loads ads, before its mediation adapters have started.
private val sdkReady = CompletableDeferred<Unit>()

internal actual fun platformRequestConfiguration(): RequestConfiguration = synchronized(configurationLock) {
    heldConfiguration
}

internal actual fun setPlatformRequestConfiguration(configuration: RequestConfiguration) {
    synchronized(configurationLock) {
        heldConfiguration = configuration
        if (sdkStarted) MobileAds.setRequestConfiguration(configuration.toAndroid())
    }
}

internal actual fun startPlatformSdk(onComplete: (InitializationStatus) -> Unit) {
    val context = applicationContext()
    val config = InitializationConfig.Builder(applicationId(context))
    // On a background thread, which the Next-Gen SDK requires: initialization reads from disk.
    thread(name = "admob-initialize") {
        val startConfiguration = synchronized(configurationLock) { heldConfiguration }
        MobileAds.initialize(context, config.setRequestConfiguration(startConfiguration.toAndroid()).build()) { status ->
            postToMainThread { onComplete(status.toCommon()) }
        }
        synchronized(configurationLock) {
            sdkStarted = true
            // What the app set while the SDK was starting.
            if (heldConfiguration != startConfiguration) MobileAds.setRequestConfiguration(heldConfiguration.toAndroid())
        }
        sdkReady.complete(Unit)
    }
}

internal actual suspend fun awaitStartedSdk() {
    AdMob.startInitialization()
    sdkReady.await()
}

internal actual fun openPlatformAdInspector(host: AdHost, onClosed: (AdError?) -> Unit) {
    // The Next-Gen SDK opens it over the app's current Activity.
    MobileAds.openAdInspector { error -> postToMainThread { onClosed(error?.toAdError()) } }
}

/** The app ID of the manifest entry the UMP SDK reads too; the Next-Gen SDK takes it in code. */
private fun applicationId(context: Context): String {
    val packageManager = context.packageManager
    val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        packageManager.getApplicationInfo(
            context.packageName,
            PackageManager.ApplicationInfoFlags.of(PackageManager.GET_META_DATA.toLong()),
        )
    } else {
        // Deprecated in Android 13 for the call above, which older versions don't have.
        @Suppress("DEPRECATION")
        packageManager.getApplicationInfo(context.packageName, PackageManager.GET_META_DATA)
    }
    return checkNotNull(info.metaData?.getString(APPLICATION_ID_KEY)) {
        "io.github.corneloaie:admob found no AdMob app ID: add <meta-data android:name=\"$APPLICATION_ID_KEY\" " +
            "android:value=\"ca-app-pub-…~…\" /> to the app's manifest."
    }
}

private const val APPLICATION_ID_KEY = "com.google.android.gms.ads.APPLICATION_ID"
