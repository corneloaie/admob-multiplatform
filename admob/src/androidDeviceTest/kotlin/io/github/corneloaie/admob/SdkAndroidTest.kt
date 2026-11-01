package io.github.corneloaie.admob

import com.google.android.libraries.ads.mobile.sdk.MobileAds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import com.google.android.libraries.ads.mobile.sdk.common.AgeRestrictedTreatment as GmaAgeRestrictedTreatment

/**
 * The GMA Next-Gen SDK as the library drives it in an app, on a device or emulator: its start on a background thread
 * with the manifest's app ID (Google's sample one), the request configuration, and loads of Google's test ad units.
 * A load passes with a fill or an error: a network that blocks ad domains gets errors.
 *
 *     ANDROID_SERIAL=emulator-5554 ./gradlew :admob:connectedAndroidDeviceTest
 */
class SdkAndroidTest {
    @Test
    fun readsTheLinkedSdkVersion() {
        val version = AdMob.sdkVersion
        println("GMA Next-Gen SDK $version")
        assertTrue(version.startsWith("1."), "unexpected GMA Next-Gen SDK version $version")
    }

    @Test
    fun initializeCompletesOnceForEveryCaller() = withSdk {
        val first = async { AdMob.initialize() }
        val second = async { AdMob.initialize() }
        val status = first.await()
        println("Initialization: $status")
        assertEquals(status, second.await())
    }

    @Test
    fun requestConfigurationReachesTheStartedSdk() = withSdk {
        AdMob.initialize()
        val configuration = RequestConfiguration(
            testDeviceIds = listOf("TEST-DEVICE"),
            ageRestrictedTreatment = AgeRestrictedTreatment.TEEN,
            maxAdContentRating = MaxAdContentRating.PG,
        )
        AdMob.requestConfiguration = configuration
        try {
            assertEquals(configuration, AdMob.requestConfiguration)
            val sdk = MobileAds.getRequestConfiguration()
            assertEquals(GmaAgeRestrictedTreatment.TEEN, sdk.ageRestrictedTreatment)
            assertTrue("TEST-DEVICE" in sdk.testDeviceIds)
        } finally {
            AdMob.requestConfiguration = RequestConfiguration()
        }
    }

    @Test
    fun consentStateMatchesTheSdk() {
        val state = AdConsent.state.value
        println("Consent: $state formAvailable=${AdConsent.isConsentFormAvailable}")
        assertEquals(AdConsent.consentStatus, state.consentStatus)
        assertEquals(AdConsent.canRequestAds, state.canRequestAds)
    }

    @Test
    fun aNativeAdLoadReportsBack() = withSdk {
        report("Native ad") { NativeAdLoader(NATIVE_UNIT).load().let { ad -> ad.responseInfo.also { ad.destroy() } } }
    }

    @Test
    fun anInterstitialLoadReportsBack() = withSdk {
        report("Interstitial") { InterstitialAd.load(INTERSTITIAL_UNIT).responseInfo }
    }

    @Test
    fun aRewardedLoadReportsBack() = withSdk {
        report("Rewarded") { RewardedAd.load(REWARDED_UNIT).let { "${it.rewardItem} ${it.responseInfo}" } }
    }

    private suspend fun report(name: String, load: suspend () -> Any?) {
        try {
            println("$name: filled, ${load()}")
        } catch (e: AdMobException) {
            println("$name: failed, ${e.error}")
        }
    }

    // Instrumentation runs a test on its own thread; the library's calls hop to the main one themselves.
    private fun withSdk(block: suspend CoroutineScope.() -> Unit) = runBlocking { withTimeout(120.seconds) { block() } }

    private companion object {
        // Google's test ad units.
        const val NATIVE_UNIT = "ca-app-pub-3940256099942544/2247696110"
        const val INTERSTITIAL_UNIT = "ca-app-pub-3940256099942544/1033173712"
        const val REWARDED_UNIT = "ca-app-pub-3940256099942544/5224354917"
    }
}
