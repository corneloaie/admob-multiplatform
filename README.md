# admob-multiplatform

[![Maven Central](https://img.shields.io/maven-central/v/io.github.corneloaie/admob?style=flat-square&label=Maven%20Central)](https://central.sonatype.com/artifact/io.github.corneloaie/admob)
[![CI](https://img.shields.io/github/actions/workflow/status/corneloaie/admob-multiplatform/ci.yml?branch=main&label=CI&style=flat-square)](.github/workflows/ci.yml)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue?style=flat-square)](LICENSE)

**Google Mobile Ads (AdMob) and the UMP consent SDK in Kotlin Multiplatform common code**, on Android and iOS. It is
built on each platform's official SDK and calls it directly. On iOS that goes through Kotlin's SwiftPM import, the same
mechanism as [firebase-multiplatform](https://github.com/corneloaie/firebase-multiplatform), so the app needs no Swift
bridge.

- **Initialization:** start the SDK and wait for its adapters. Also the request configuration (test devices,
  child-directed and under-age tags, max ad content rating), the SDK version and the Ad Inspector.
- **Consent:** UMP's info update, consent form, privacy options form, `canRequestAds` and reset, with the state as a
  `StateFlow`.
- **Native ads:** a loader with options. Each loaded ad has its assets, response info and an event `Flow` (impression,
  click, paid). Rendering stays native, with the platform ad, plus a few iOS view helpers.
- **Interstitial and rewarded ads:** a suspending `show` that returns when the ad is closed (with the reward), and the
  same events.
- **Suspending functions and Flows,** with common enums and error codes. Android and iOS number their constants
  differently, and the library maps each one explicitly.

What stays in the app: ad card layouts, caches of loaded ads, when to show a full-screen ad, and where revenue events
go (RevenueCat, analytics).

## Getting started

### Gradle

```kotlin
// build.gradle.kts of the shared module
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("io.github.corneloaie:admob:0.1.0")
        }
    }
}
```

The library has Android, `iosArm64` and `iosSimulatorArm64` targets only. If the shared module also targets the JVM, JS
or Wasm, add the dependency to a source set that only Android and iOS share (for example, a `mobileMain` source set
that `androidMain` and `iosMain` depend on).

### Android

- The minimum SDK is 24. The library brings `play-services-ads` 25.5.0 and `user-messaging-platform` 4.0.0 as `api`
  dependencies.
- Add the AdMob app id to the app's manifest. Without it, the Google Mobile Ads SDK crashes the app at startup:

  ```xml
  <application>
      <meta-data
          android:name="com.google.android.gms.ads.APPLICATION_ID"
          android:value="ca-app-pub-xxxxxxxxxxxxxxxx~yyyyyyyyyy" />
  </application>
  ```

- A small `ContentProvider` in the library's manifest (`AdMobInitProvider`, not exported) keeps the application
  `Context` at startup. Loading ads and reading the consent state therefore need no `Context`.

### iOS

The library binds to the official
[GoogleMobileAds](https://github.com/googleads/swift-package-manager-google-mobile-ads) and
[UMP](https://github.com/googleads/swift-package-manager-google-user-messaging-platform) Swift packages without bundling
them. Its Maven publication records both packages, and your app inherits them: GoogleMobileAds from 13.3.0 and UMP from
3.0.0, each up to the next major version. You don't add either package to Xcode yourself.

- **Requirements:** Kotlin 2.4 or newer and Xcode 26.2 or newer (the library is built and tested with Kotlin 2.4.21 and
  Xcode 26.6). The minimum deployment target is iOS 15.
- **Integration:** use
  [direct integration](https://kotlinlang.org/docs/multiplatform/multiplatform-direct-integration.html), the
  `embedAndSignAppleFrameworkForXcode` build phase, and build the shared framework as static
  (`binaries.framework { isStatic = true }`). The Swift packages are static libraries, and a dynamic framework fails at
  launch with `dyld: Library not loaded`.
- **Linkage package:** the Kotlin Gradle plugin links the inherited packages through a generated Swift package that
  your Xcode project has to include. Until it does, the Xcode build stops with "SwiftPM linkage package not integrated
  into Xcode project" and prints the command to run. That command runs the `integrateEmbedAndSign` and
  `integrateLinkagePackage` tasks against your Xcode project; commit the project change it makes. An app that already
  consumes firebase-multiplatform has done this once, and the same package now carries the ads SDKs too. Kotlin's
  [CocoaPods to SwiftPM migration guide](https://kotl.in/cocoapods-to-swiftpm-migration) and
  firebase-multiplatform's
  [iOS linking notes](https://github.com/corneloaie/firebase-multiplatform/blob/main/documentation/ios-firebase-linking.md)
  explain the mechanism.
- **Info.plist:** add `GADApplicationIdentifier` (your AdMob app id) and the `SKAdNetworkItems` that Google lists in
  [its iOS quick start](https://developers.google.com/admob/ios/quick-start#update_your_infoplist).
- **Mediation adapters, or GoogleMobileAds pinned in Xcode:** SwiftPM resolves one GoogleMobileAds for the whole app.
  The library accepts any 13.x from 13.3.0 on, and its CI tests both 13.3.0 exactly and the newest 13.x. An app that
  pins GoogleMobileAds 13.3.0 itself, for example for the Meta Audience Network adapter, leaves SwiftPM a version both
  accept.
- **After the resolved SDK version changes,** for example when you upgrade this library, Kotlin 2.4.20 can fail with
  "module.modulemap has been modified since the module file ... was built"
  ([KT-88106](https://youtrack.jetbrains.com/issue/KT-88106)). Delete `build/kotlin/swiftPMXcodeDumps` in the root
  project.

## Usage

Every call that shows something takes an `AdHost`: the `Activity` on Android, the `UIViewController` on iOS. Every other
call needs none. In Compose Multiplatform:

```kotlin
// commonMain
@Composable
expect fun currentAdHost(): AdHost

// androidMain (androidx.activity:activity-compose)
@Composable
actual fun currentAdHost(): AdHost = checkNotNull(LocalActivity.current) { "not hosted in an Activity" }

// iosMain
@Composable
actual fun currentAdHost(): AdHost = LocalUIViewController.current
```

The suspending calls switch to the main thread themselves. They throw `AdMobException` when the SDK reports an error;
its `error.code` is an `AdErrorCode` that is the same on both platforms (`NO_FILL`, `NETWORK_ERROR`, `TIMEOUT`, ...).
`error.platformCode` and `error.domain` keep the SDK's own values.

### Consent and initialization

Google's order is: update the consent info at every launch, show the form if consent is required, then start the SDK
once ads may be requested.

```kotlin
suspend fun startAds(host: AdHost) {
    try {
        AdConsent.requestInfoUpdate(host)
        AdConsent.loadAndShowConsentFormIfRequired(host)
    } catch (e: AdMobException) {
        // Offline, for example. Consent given in an earlier session still counts: check canRequestAds.
    }
    if (AdConsent.canRequestAds) {
        AdMob.requestConfiguration = RequestConfiguration(maxAdContentRating = MaxAdContentRating.PG)
        val status = AdMob.initialize() // returns once the mediation adapters have started
    }
}
```

`AdConsent.state` is a `StateFlow<ConsentState>` (`consentStatus`, `canRequestAds`, `privacyOptionsRequirementStatus`).
It updates as soon as `requestInfoUpdate` is called, not only when the request completes, because UMP loads the consent
saved in an earlier session right away. A returning user's ads therefore don't wait for the network.

```kotlin
val canRequestAds: StateFlow<Boolean> =
    AdConsent.state.map { it.canRequestAds }.stateIn(scope, SharingStarted.Eagerly, false)

// A privacy settings entry, shown when the user must be able to change their choices:
if (AdConsent.privacyOptionsRequirementStatus == PrivacyOptionsRequirementStatus.REQUIRED) {
    AdConsent.showPrivacyOptionsForm(host)
}

// Testing: appear in the EEA, and forget this device's answer.
AdConsent.requestInfoUpdate(
    host,
    ConsentRequestParameters(debugSettings = ConsentDebugSettings(DebugGeography.EEA, listOf("<hashed device id>"))),
)
AdConsent.reset()
```

`AdMob.initialize()` starts the SDK once; later and concurrent calls wait for the same result. Set
`AdMob.requestConfiguration` before it. To use the Ad Inspector, add the device to `testDeviceIds` and call
`AdMob.openAdInspector(host)`.

### Native ads

```kotlin
val loader = NativeAdLoader(
    adUnitId,
    NativeAdOptions(
        mediaAspectRatio = MediaAspectRatio.LANDSCAPE,
        adChoicesPlacement = AdChoicesPlacement.TOP_RIGHT,
        startMuted = true,
    ),
)

val ad: NativeAd = loader.load() // throws AdMobException, e.g. AdErrorCode.NO_FILL
ad.headline; ad.body; ad.callToAction; ad.icon; ad.mediaContent?.aspectRatio; ad.responseInfo?.responseId

scope.launch {
    ad.events.collect { event ->
        when (event) {
            AdEvent.Impression -> {}
            AdEvent.Click -> {}
            is AdEvent.Paid -> event.value.let { it.valueMicros; it.currencyCode; it.precision }
            else -> {}
        }
    }
}

ad.destroy() // once it is off screen for good; completes ad.events
```

The library wires the SDK's listeners when the ad loads, before `load` returns. Every collector of `events` receives
every event from the start, however late it starts collecting. The paid event, which the SDK only delivers to a
listener that was in place at impression time, is never lost. Each `load` is a request of its own; caching ads and
deciding when to load one stay in the app.

**Rendering** stays native, with the platform ad.

On Android, register `ad.android` (a `com.google.android.gms.ads.nativead.NativeAd`) with your `NativeAdView`:

```kotlin
// androidMain, e.g. in an AndroidView's update block; headline and mediaView are children of nativeAdView
nativeAdView.headlineView = headline
nativeAdView.mediaView = mediaView
headline.text = ad.headline
nativeAdView.setNativeAd(ad.android)
```

On iOS, `ad.ios` is the `GADNativeAd`. The library adds three helpers for a card built in Kotlin:

```kotlin
// iosMain
val card = AttachRegisteringNativeAdView()
val headline = UILabel().apply { text = ad.headline }
val media = GADMediaView()
val badge = AdBadgeLabel().apply { text = "Ad" }
// The asset outlets are weak: add each view to the card first, then register it (Auto Layout constraints omitted).
listOf(headline, media, badge).forEach(card::addSubview)
card.headlineView = headline
card.mediaView = media
card.registerWhenAttached(ad) // sets nativeAd once the card is in a window and laid out
val height = preferredHeight(card, width = 360.0) // to frame its Compose UIKitView
```

- `AttachRegisteringNativeAdView` sets its `nativeAd` only once it is in a window and laid out. A card set before then
  (as Compose's `UIKitView` builds it) measures 0×0, and the SDK's ad validator reports its asset views as missing.
- `AdBadgeLabel` never gets smaller than the 15pt the validator requires for the "Ad" attribution.
- `preferredHeight` returns the card's Auto Layout height at a given width. The media view follows the creative's aspect
  ratio, so every card has a different height.

### Interstitial and rewarded ads

```kotlin
val interstitial = InterstitialAd.load(interstitialUnitId)
interstitial.show(host) // returns when the user closes it

val rewarded = RewardedAd.load(rewardedUnitId)
rewarded.rewardItem // what the ad unit grants, e.g. RewardItem("coins", 10)
val reward: RewardItem? = rewarded.show(host) // null if they closed it before earning the reward
```

A full-screen ad shows once. Its `events` run from `Shown` to `Dismissed` (or `FailedToShow`), with `Impression`,
`Click`, `Paid` and, for a rewarded ad, `RewardEarned` in between. The flow then completes. `show` throws
`AdMobException` when the ad can't be shown, for example `AdErrorCode.ALREADY_USED` or `NOT_READY`.
`RewardedAd.setServerSideVerificationOptions` sets the data for AdMob's server-side verification callback.

### Revenue and ad events

Every ad carries what an ad revenue tracker such as RevenueCat's needs. `responseInfo.responseId` is the per-fill id,
`responseInfo.loadedAdapterResponseInfo?.adSourceName` (or `mediationAdapterClassName`) is the network that filled the
ad, and `AdEvent.Paid` carries the value in micros, the currency and the precision. Impression-level ad revenue must be
on in the AdMob account settings for the SDK to report paid events.

### The platform SDK objects

`NativeAd`, `InterstitialAd` and `RewardedAd` have an `android` or `ios` property holding the SDK's own ad, readable in
that platform's source set, for anything common code doesn't reach.

## Building and testing

```bash
./gradlew :admob:testAndroidHostTest                  # common and Android tests on the JVM
./gradlew :admob:iosSimulatorArm64Test                # common and iOS tests on a simulator, against the real SDKs
./gradlew -Padmob.pinIosSdkFloor :admob:iosSimulatorArm64Test   # the same, against GoogleMobileAds 13.3.0 and UMP 3.0.0 exactly
./gradlew :admob:checkKotlinAbi                       # the public API against api/ (updateKotlinAbi rewrites it)
./gradlew :admob:publishToMavenLocal
```

- **Simulator tests:** the test executable embeds an Info.plist with Google's sample app id, and the tests request
  Google's public test ad units. A test executable isn't an app, and the SDKs' requests from it fail even on an open
  network, as on CI: UMP with a TLS error, the ad requests with `TIMEOUT`. The tests therefore check that every call
  reports back through the library, with a fill or an error. A filled ad needs an app.
- **SDK version switches:** after switching between the newest SDKs and the floor, or changing a version, start clean
  with `rm -rf .swiftpm-locks build admob/build`. SwiftPM keeps a locked version its range still allows, Gradle doesn't
  relink when only the resolved SDK changes, and stale Xcode dumps fail the build (KT-88106).

[MAINTAINING.md](MAINTAINING.md) covers releases.

## License

Apache License 2.0, see [LICENSE](LICENSE). The library binds to, but doesn't include, the Google Mobile Ads and User
Messaging Platform SDKs, which are Google's and licensed under their own terms.
