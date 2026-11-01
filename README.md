# admob-multiplatform

Kotlin Multiplatform access to Google Mobile Ads (AdMob) and the UMP consent SDK on Android and iOS. On iOS the
Obj-C SDKs come in through Kotlin's SwiftPM import (`swiftPMDependencies`), the same mechanism as
[firebase-multiplatform](https://github.com/corneloaie/firebase-multiplatform): no Swift bridge in the app.

**Status: spike.** `admob/src/iosMain/.../spike/` holds probes, not the API.

## What the spike proved (2026-11-01: Kotlin 2.4.21, AGP 9.4.1, Gradle 9.8.2, Xcode 26.6)

- **Resolve + cinterop.** `swiftPMDependencies` resolves GoogleMobileAds `from 13.3.0` and UMP `from 3.0.0` (resolved
  13.11.0 and 3.1.0). The whole Obj-C API is visible under `swiftPMImport.io.github.corneloaie.admob`, with blocks
  as Kotlin lambdas and NSArrays as `List`.
- **InfoTraffic's Swift ad code has a Kotlin equivalent.** These compile for `iosSimulatorArm64` and `iosArm64`:
  - `ConsentProbe` (the Swift `AdsConsentCoordinator`): consent info update, consent form, privacy options, reset,
    `MobileAds.start`, Ad Inspector.
  - `NativeAdSession` (the Swift `AdSlotSession`): an NSObject implementing `GADNativeAdLoaderDelegate` +
    `GADNativeAdDelegate`, the paid-event block in micros, and `ResponseInfo` (response id, network name).
  - `AttachRegisteringNativeAdView`: a Kotlin subclass of `GADNativeAdView` overriding `didMoveToWindow`.
  - `AdBadgeLabel` + `preferredHeight`: a `UILabel` subclass overriding struct-taking and struct-returning methods
    (`drawTextInRect`, `intrinsicContentSize` → `CGSize`), plus `systemLayoutSizeFitting`. The ad card builder can
    therefore move to Kotlin too. Kotlin/Native gotcha: a subclass of an Obj-C type can't have fields in its
    companion object, so constants go at file level.
- **It links and runs.** `iosSimulatorArm64Test` links the real binaries and passes:
  - the SDK version (13.11.0)
  - the UMP consent status
  - the view subclass
  - the badge label's size
  - a request for Google's test native ad that reaches the Kotlin delegate

  The test executable embeds an Info.plist (`-sectcreate __TEXT __info_plist`) so GMA finds
  `GADApplicationIdentifier`.
- **Consumers inherit the Swift packages.** The published Gradle module has a `swiftPMDependenciesMetadataElements`
  variant with both packages, so a consuming app inherits them as it does `firebase-ios-sdk`.
- **Android compiles** against play-services-ads 25.5.0 + UMP 4.0.0.

## Not proven yet

- **A filled ad.** The test request timed out (`GADErrorTimeout`, delivered to the Kotlin delegate). The likely cause
  is AdGuard on the dev Mac, which resolves `googleads.g.doubleclick.net` to `0.0.0.0`. That isn't confirmed: a bare
  test executable with no `UIApplication` could also time out. The fill path is still unseen: rendering, deferred
  registration, impression, click and paid callbacks. Pause AdGuard and rerun `NativeAdLoadSpikeTest`. If it still
  times out, test the fill in an app instead.
- **Use from an app.** Still to check:
  - the KGP linkage package
  - the app's own GoogleMobileAds pin and the Meta mediation adapter resolving to one GMA
  - a Release archive

  InfoTraffic can consume it today: Kotlin 2.4.21 and the linkage package are already there for the Firebase lib.
  PocketOracle first needs Kotlin 2.4+ (it's on 2.3.20), and its iOS app needs the linkage package integrated.

## Commands

```bash
./gradlew :admob:iosSimulatorArm64Test
./gradlew :admob:compileAndroidMain :admob:compileKotlinIosArm64
./gradlew :admob:publishAllPublicationsToSpikeRepository   # build/spike-repo, to inspect the module metadata
```

After changing an iOS SDK version, run `rm -rf build/kotlin/swiftPMXcodeDumps` (KT-88106, as in the Firebase lib).
