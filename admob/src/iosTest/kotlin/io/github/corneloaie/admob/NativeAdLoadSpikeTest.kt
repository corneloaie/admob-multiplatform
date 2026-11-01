package io.github.corneloaie.admob

import io.github.corneloaie.admob.spike.ConsentProbe
import io.github.corneloaie.admob.spike.NativeAdSession
import io.github.corneloaie.admob.spike.networkName
import platform.Foundation.NSDate
import platform.Foundation.NSRunLoop
import platform.Foundation.NSThread
import platform.Foundation.dateWithTimeIntervalSinceNow
import platform.Foundation.runUntilDate
import platform.Foundation.timeIntervalSinceNow
import swiftPMImport.io.github.corneloaie.admob.GADNativeAd
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Requests one of Google's public test native ads and waits for the Kotlin delegate. Either callback proves the SDK
 * dispatches into a Kotlin NSObject (fill → adLoader(_:didReceive:), no fill / network → didFailToReceiveAdWithError).
 */
class NativeAdLoadSpikeTest {
    @Test
    fun aTestNativeAdRequestReachesTheKotlinDelegate() {
        assertTrue(NSThread.isMainThread, "GMA wants the main thread")
        ConsentProbe.startMobileAds()

        var loaded: GADNativeAd? = null
        var failedCode: Long? = null
        val session = NativeAdSession(
            adUnitId = TEST_NATIVE_AD_UNIT,
            onLoaded = { loaded = it },
            onImpression = {},
            onClick = {},
            onPaid = {},
            onFailed = { failedCode = it },
        )
        session.start()

        val deadline = NSDate.dateWithTimeIntervalSinceNow(30.0)
        while (loaded == null && failedCode == null && deadline.timeIntervalSinceNow > 0) {
            NSRunLoop.mainRunLoop.runUntilDate(NSDate.dateWithTimeIntervalSinceNow(0.25))
        }

        val ad = loaded
        when {
            ad != null -> println(
                "Native ad filled: headline='${ad.headline}' cta='${ad.callToAction}' " +
                    "network=${ad.responseInfo.networkName} responseId=${ad.responseInfo.responseIdentifier}",
            )
            failedCode != null -> println("Native ad request failed in the Kotlin delegate, GADErrorCode=$failedCode")
            else -> fail("neither delegate callback fired within 30 s")
        }
    }

    private companion object {
        // Google's public native advanced test unit (iOS).
        const val TEST_NATIVE_AD_UNIT = "ca-app-pub-3940256099942544/3986624511"
    }
}
