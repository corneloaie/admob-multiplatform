package io.github.corneloaie.admob

import io.github.corneloaie.admob.spike.AdBadgeLabel
import io.github.corneloaie.admob.spike.AttachRegisteringNativeAdView
import io.github.corneloaie.admob.spike.ConsentProbe
import io.github.corneloaie.admob.spike.preferredHeight
import kotlinx.cinterop.useContents
import kotlin.test.Test
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Runs on a simulator against the real, SwiftPM-linked GMA + UMP binaries: proves they link and answer at runtime. */
class InteropSpikeTest {
    @Test
    fun readsTheLinkedSdkVersion() {
        val version = AdMob.sdkVersion
        println("GoogleMobileAds version: $version")
        assertTrue(version.startsWith("13."), "unexpected GMA version: $version")
    }

    @Test
    fun readsConsentState() {
        println("UMP consentStatus=${ConsentProbe.consentStatus} canRequestAds=${ConsentProbe.canRequestAds}")
        assertTrue(ConsentProbe.consentStatus in 0L..3L)
    }

    @Test
    fun subclassesTheSdkNativeAdView() {
        val view = AttachRegisteringNativeAdView()
        assertNull(view.nativeAd)
        assertNull(view.pendingAd)
    }

    @Test
    fun overridesStructReturningUiKitMethods() {
        val badge = AdBadgeLabel().apply { text = "Ad" }
        val (width, height) = badge.intrinsicContentSize().useContents { width to height }
        println("AdBadgeLabel intrinsicContentSize=${width}x$height preferredHeight=${preferredHeight(badge, 100.0)}")
        assertTrue(width >= 16.0 && height >= 16.0, "badge must stay at least 16pt: ${width}x$height")
    }
}
