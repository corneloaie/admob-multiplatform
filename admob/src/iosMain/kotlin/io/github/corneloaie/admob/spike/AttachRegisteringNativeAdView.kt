package io.github.corneloaie.admob.spike

import platform.CoreGraphics.CGRectMake
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import swiftPMImport.io.github.corneloaie.admob.GADNativeAdView

/**
 * Spike: InfoTraffic's Swift `AttachRegisteringNativeAdView` as a Kotlin subclass of the SDK's Obj-C GADNativeAdView,
 * overriding a UIKit lifecycle method. Assigning `nativeAd` is deferred until the view is in a window, so the SDK
 * inspects a laid-out hierarchy (zero-sized assets make its validator report pub-rendered views as missing).
 */
internal class AttachRegisteringNativeAdView : GADNativeAdView(frame = CGRectMake(0.0, 0.0, 0.0, 0.0)) {
    var pendingAd: swiftPMImport.io.github.corneloaie.admob.GADNativeAd? = null

    override fun didMoveToWindow() {
        super.didMoveToWindow()
        if (window == null || pendingAd == null) return
        dispatch_async(dispatch_get_main_queue()) {
            val ad = pendingAd
            if (window == null || ad == null) return@dispatch_async
            pendingAd = null
            layoutIfNeeded()
            nativeAd = ad
        }
    }
}
