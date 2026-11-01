package io.github.corneloaie.admob

import kotlinx.cinterop.useContents
import kotlin.test.Test
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NativeAdViewsIosTest {
    @Test
    fun theAdViewSubclassStartsWithoutAnAd() {
        assertNull(AttachRegisteringNativeAdView().nativeAd)
    }

    @Test
    fun theBadgeNeverShrinksBelowWhatTheValidatorAccepts() {
        val badge = AdBadgeLabel().apply { text = "Ad" }
        val (width, height) = badge.intrinsicContentSize().useContents { width to height }
        val preferred = preferredHeight(badge, width = 100.0)
        println("AdBadgeLabel intrinsicContentSize=${width}x$height preferredHeight=$preferred")
        assertTrue(width >= 16.0 && height >= 16.0, "the badge must stay at least 16pt: ${width}x$height")
        assertTrue(preferred >= 16.0, "preferredHeight=$preferred")
    }
}
