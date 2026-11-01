package io.github.corneloaie.admob

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Loads native ads for one ad unit. Each [load] is a request of its own, so concurrent loads are fine.
 *
 * ```
 * val loader = NativeAdLoader(adUnitId, NativeAdOptions(mediaAspectRatio = MediaAspectRatio.LANDSCAPE))
 * val ad = loader.load()
 * ```
 */
public class NativeAdLoader(
    public val adUnitId: String,
    public val options: NativeAdOptions = NativeAdOptions(),
) {
    /**
     * Requests an ad and returns it once it has loaded. Its events are wired before it is returned, so none is lost.
     * Cancelling the call destroys an ad that arrives afterwards.
     *
     * @throws AdMobException when the request fails, e.g. [AdErrorCode.NO_FILL].
     */
    public suspend fun load(request: AdRequest = AdRequest()): NativeAd =
        withContext(Dispatchers.Main.immediate) { loadPlatformNativeAd(adUnitId, options, request) }
}

/**
 * How native ads are requested. A null option leaves the SDK's default.
 *
 * @property mediaAspectRatio the main media's preferred shape.
 * @property adChoicesPlacement the corner the SDK puts the AdChoices icon in.
 * @property startMuted whether video ads start muted (the SDK's default).
 */
public data class NativeAdOptions(
    val mediaAspectRatio: MediaAspectRatio? = null,
    val adChoicesPlacement: AdChoicesPlacement? = null,
    val startMuted: Boolean? = null,
)

public enum class MediaAspectRatio { ANY, LANDSCAPE, PORTRAIT, SQUARE }

public enum class AdChoicesPlacement { TOP_LEFT, TOP_RIGHT, BOTTOM_RIGHT, BOTTOM_LEFT }

internal expect suspend fun loadPlatformNativeAd(adUnitId: String, options: NativeAdOptions, request: AdRequest): NativeAd
