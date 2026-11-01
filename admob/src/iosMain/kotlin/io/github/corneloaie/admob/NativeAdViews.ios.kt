package io.github.corneloaie.admob

import kotlinx.cinterop.CValue
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGRect
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSize
import platform.CoreGraphics.CGSizeMake
import platform.UIKit.UIEdgeInsetsInsetRect
import platform.UIKit.UIEdgeInsetsMake
import platform.UIKit.UILabel
import platform.UIKit.UILayoutFittingCompressedSize
import platform.UIKit.UILayoutPriorityFittingSizeLevel
import platform.UIKit.UILayoutPriorityRequired
import platform.UIKit.UIView
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import swiftPMImport.io.github.corneloaie.admob.GADNativeAd
import swiftPMImport.io.github.corneloaie.admob.GADNativeAdView
import kotlin.math.ceil
import kotlin.math.max

/**
 * A `GADNativeAdView` that registers its ad only once it is in a window and laid out.
 *
 * The SDK inspects the view hierarchy when `nativeAd` is set. A card built before it is attached (as Compose's
 * `UIKitView` does) measures every asset view as 0×0 then, and the SDK's ad validator reports the views as missing
 * ("MediaView not used", "Ad attribution missing"). Registering on attach, one run loop turn after the container has
 * set the frame, gives the SDK a laid-out hierarchy, and that is also when impression tracking should start.
 *
 * Build the card as with any `GADNativeAdView` (asset views, `headlineView`, `mediaView`, ...), then call
 * [registerWhenAttached] instead of setting `nativeAd`.
 */
public class AttachRegisteringNativeAdView : GADNativeAdView(frame = CGRectMake(0.0, 0.0, 0.0, 0.0)) {
    private var pendingAd: GADNativeAd? = null

    /** Sets [ad] as this view's `nativeAd` once the view is in a window: now, if it already is. */
    public fun registerWhenAttached(ad: NativeAd) {
        pendingAd = ad.ios
        if (window != null) registerPendingAd()
    }

    override fun didMoveToWindow() {
        super.didMoveToWindow()
        if (window != null && pendingAd != null) registerPendingAd()
    }

    private fun registerPendingAd() {
        dispatch_async(dispatch_get_main_queue()) {
            val ad = pendingAd
            if (window == null || ad == null) return@dispatch_async
            pendingAd = null
            layoutIfNeeded()
            nativeAd = ad
        }
    }
}

/**
 * A pill-padded label for a native ad's "Ad" attribution badge. AdMob's ad validator wants the badge at least 15pt in
 * both dimensions, and reports "Ad attribution missing" below that: this label is never smaller than 16pt.
 */
public class AdBadgeLabel : UILabel(frame = CGRectMake(0.0, 0.0, 0.0, 0.0)) {
    private val insets = UIEdgeInsetsMake(top = 2.0, left = 6.0, bottom = 2.0, right = 6.0)

    override fun drawTextInRect(rect: CValue<CGRect>) {
        super.drawTextInRect(UIEdgeInsetsInsetRect(rect, insets))
    }

    override fun intrinsicContentSize(): CValue<CGSize> {
        val (width, height) = super.intrinsicContentSize().useContents { width to height }
        val (horizontal, vertical) = insets.useContents { (left + right) to (top + bottom) }
        return CGSizeMake(max(width + horizontal, BADGE_MIN_SIDE), max(height + vertical, BADGE_MIN_SIDE))
    }
}

// Top level, not a companion: Kotlin/Native rejects fields in the companion of an Obj-C subclass.
private const val BADGE_MIN_SIDE = 16.0

/**
 * The Auto Layout height of [view], an ad card, laid out at [width] points: what to frame its Compose `UIKitView` with.
 * The media view takes the creative's aspect ratio, so every card is a different height.
 *
 * `translatesAutoresizingMaskIntoConstraints` is off while measuring: a card sized by frame keeps it on, and measuring
 * with it on would pin the (still 0×0) frame as required constraints.
 */
public fun preferredHeight(view: UIView, width: Double): Double {
    val wasTranslating = view.translatesAutoresizingMaskIntoConstraints
    view.translatesAutoresizingMaskIntoConstraints = false
    try {
        // A global C struct constant: Kotlin/Native exposes it as the struct itself, not a CValue.
        val fitting = UILayoutFittingCompressedSize.height
        return view.systemLayoutSizeFittingSize(
            CGSizeMake(width, fitting),
            withHorizontalFittingPriority = UILayoutPriorityRequired,
            verticalFittingPriority = UILayoutPriorityFittingSizeLevel,
        ).useContents { ceil(height) }
    } finally {
        view.translatesAutoresizingMaskIntoConstraints = wasTranslating
    }
}
