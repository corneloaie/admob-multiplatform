package io.github.corneloaie.admob

import kotlinx.coroutines.async
import kotlinx.coroutines.flow.toList
import platform.UIKit.UIViewController
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Runs on a simulator against the real, SwiftPM-linked SDKs. The test executable embeds an Info.plist with Google's
 * sample app id, and the ad requests go to Google's public test ad units. A test executable isn't an app: its network
 * requests fail even on an open network, as on CI (UMP with a TLS error, ads with [AdErrorCode.TIMEOUT]), so the
 * tests check that each call reports back through the library, with a fill or an error.
 */
class SdkIosTest {
    @AfterTest
    fun resetRequestConfiguration() {
        AdMob.requestConfiguration = RequestConfiguration()
    }

    @Test
    fun readsTheLinkedSdkVersion() {
        val version = AdMob.sdkVersion
        println("GoogleMobileAds $version")
        assertTrue(version.startsWith("13."), "unexpected GoogleMobileAds version $version")
    }

    @Test
    fun requestConfigurationRoundTripsThroughTheSdk() {
        val configuration = RequestConfiguration(
            testDeviceIds = listOf("TEST-DEVICE"),
            ageRestrictedTreatment = AgeRestrictedTreatment.TEEN,
            maxAdContentRating = MaxAdContentRating.PG,
        )
        AdMob.requestConfiguration = configuration
        assertEquals(configuration, AdMob.requestConfiguration)

        AdMob.requestConfiguration = RequestConfiguration()
        assertEquals(RequestConfiguration(), AdMob.requestConfiguration)
    }

    @Test
    fun initializeCompletesOnceForEveryCaller() = runOnMainLoop(timeoutSeconds = 120.0) {
        val first = async { AdMob.initialize() }
        val second = async { AdMob.initialize() }
        val status = first.await()
        println("Initialization: $status")
        assertEquals(status, second.await())
        assertTrue(status.adapterStatuses.isNotEmpty(), "the SDK reports at least its own adapter")
    }

    @Test
    fun consentStateMatchesTheSdk() {
        val state = AdConsent.state.value
        println("Consent: $state formAvailable=${AdConsent.isConsentFormAvailable}")
        assertEquals(AdConsent.consentStatus, state.consentStatus)
        assertEquals(AdConsent.canRequestAds, state.canRequestAds)
    }

    @Test
    fun consentInfoUpdateReportsBack() = runOnMainLoop {
        val outcome = runCatching { AdConsent.requestInfoUpdate(UIViewController()) }
        println("Consent update: ${outcome.exceptionOrNull()?.let { (it as? AdMobException)?.error ?: it } ?: "ok"}")
        outcome.exceptionOrNull()?.let { assertIs<AdMobException>(it) }
        assertEquals(AdConsent.canRequestAds, AdConsent.state.value.canRequestAds)
    }

    @Test
    fun nativeAdRequestEndsInAnAdOrAnError() = runOnMainLoop(timeoutSeconds = 90.0) {
        val loader = NativeAdLoader(TEST_NATIVE, NativeAdOptions(mediaAspectRatio = MediaAspectRatio.LANDSCAPE))
        val outcome = runCatching {
            val ad = loader.load()
            assertEquals(TEST_NATIVE, ad.adUnitId)
            val events = async { ad.events.toList() }
            val summary = "headline='${ad.headline}' media=${ad.mediaContent} response=${ad.responseInfo}"
            ad.destroy()
            "loaded, $summary events=${events.await()}"
        }
        report("Native ad", outcome)
    }

    @Test
    fun interstitialRequestEndsInAnAdOrAnError() = runOnMainLoop(timeoutSeconds = 90.0) {
        val outcome = runCatching { InterstitialAd.load(TEST_INTERSTITIAL) }
        report("Interstitial", outcome.map { "loaded, response=${it.responseInfo}" })
    }

    @Test
    fun rewardedRequestEndsInAnAdOrAnError() = runOnMainLoop(timeoutSeconds = 90.0) {
        val outcome = runCatching { RewardedAd.load(TEST_REWARDED) }
        report("Rewarded", outcome.map { "loaded, reward=${it.rewardItem} response=${it.responseInfo}" })
    }

    private fun report(format: String, outcome: Result<String>) {
        outcome.exceptionOrNull()?.let { assertIs<AdMobException>(it, "$format: ${it.message}") }
        println("$format: ${outcome.getOrElse { "failed, ${(it as AdMobException).error}" }}")
    }

    private companion object {
        // Google's public test ad units for iOS.
        const val TEST_NATIVE = "ca-app-pub-3940256099942544/3986624511"
        const val TEST_INTERSTITIAL = "ca-app-pub-3940256099942544/4411468910"
        const val TEST_REWARDED = "ca-app-pub-3940256099942544/1712485313"
    }
}
