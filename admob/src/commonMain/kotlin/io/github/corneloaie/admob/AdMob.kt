package io.github.corneloaie.admob

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** The Google Mobile Ads SDK: starting it, the settings every request carries, and its debugging tools. */
public object AdMob {
    /** The Google Mobile Ads SDK version the app links, as the SDK reports it, e.g. `25.5.0` or `13.11.0`. */
    public val sdkVersion: String get() = platformSdkVersion()

    /**
     * What every ad request carries: test devices, the child-directed and under-age tags and the maximum ad content
     * rating. Set it before [initialize] and before loading ads. Reading it returns what the SDK holds.
     */
    public var requestConfiguration: RequestConfiguration
        get() = platformRequestConfiguration()
        set(value) = setPlatformRequestConfiguration(value)

    /**
     * Starts the SDK and its mediation adapters, and returns once they have all started or timed out.
     *
     * Only the first call starts the SDK; later and concurrent calls wait for the same result, and cancelling a caller
     * doesn't cancel the start. With consent to gather, start the SDK once [AdConsent.canRequestAds] is true. With
     * mediation, wait for this before loading ads, so every adapter can bid. On Android the SDK starts on a background
     * thread, as Google recommends.
     */
    public suspend fun initialize(): InitializationStatus = initialization.await()

    /**
     * Opens the Ad Inspector over [host] and returns when it closes. It opens only on a test device (see
     * [RequestConfiguration.testDeviceIds]).
     *
     * @throws AdMobException when it can't open, e.g. [AdErrorCode.NOT_IN_TEST_MODE].
     */
    public suspend fun openAdInspector(host: AdHost): Unit = withContext(Dispatchers.Main.immediate) {
        suspendCancellableCoroutine { continuation ->
            openPlatformAdInspector(host) { error ->
                if (error == null) continuation.resume(Unit) else continuation.resumeWithException(AdMobException(error))
            }
        }
    }

    private val initialization = OnceInitializer(::startPlatformSdk)
}

internal expect fun platformSdkVersion(): String

internal expect fun platformRequestConfiguration(): RequestConfiguration

internal expect fun setPlatformRequestConfiguration(configuration: RequestConfiguration)

/** Starts the SDK; [onComplete] runs on the main thread, also when called from another one. */
internal expect fun startPlatformSdk(onComplete: (InitializationStatus) -> Unit)

internal expect fun openPlatformAdInspector(host: AdHost, onClosed: (AdError?) -> Unit)
