package io.github.corneloaie.admob

/**
 * The screen the SDKs present their UI from: an `android.app.Activity` on Android, a `UIViewController` on iOS.
 *
 * Only calls that show something take one: [AdConsent.requestInfoUpdate] (UMP on Android requires an Activity), the
 * consent and privacy options forms, full-screen ads and the Ad Inspector. Loading ads and reading the consent state
 * don't. In Compose Multiplatform, `LocalActivity.current` and `LocalUIViewController.current` provide it.
 */
public expect open class AdHost
