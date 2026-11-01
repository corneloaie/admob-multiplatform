package io.github.corneloaie.admob.spike

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
import kotlin.math.ceil
import kotlin.math.max

/**
 * Spike: InfoTraffic's Swift `AdBadgeLabel` (iOSApp.swift) — UIKit overrides that take and return C structs, which
 * decide whether the ad card builder can move to Kotlin too or keeps a thin Swift layer.
 */
internal class AdBadgeLabel : UILabel(frame = CGRectMake(0.0, 0.0, 0.0, 0.0)) {
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
// The SDK's ad validator wants the attribution badge at least 15pt in both dimensions.
private const val BADGE_MIN_SIDE = 16.0

/** InfoTraffic's `preferredHeight(view:width:)`: the card's Auto Layout height at a given width. */
internal fun preferredHeight(view: UIView, width: Double): Double {
    val wasTamic = view.translatesAutoresizingMaskIntoConstraints
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
        view.translatesAutoresizingMaskIntoConstraints = wasTamic
    }
}
