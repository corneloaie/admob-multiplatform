# Maintaining admob-multiplatform

One module, `:admob`, published as `io.github.corneloaie:admob`. What sets the project up is kept in a few places:

| What | Where |
|---|---|
| Maven coordinates, POM, signing | `admob/build.gradle.kts` (`mavenPublishing`), the root `build.gradle.kts` |
| Version | `version` in `gradle.properties` |
| SDK versions | `gradle/libs.versions.toml` |
| Public API | `admob/api/` (Kotlin ABI validation) |
| CI | `.github/workflows/ci.yml` |
| Publishing | `.github/workflows/publish.yml`, `.github/scripts/publish/` |

The Kotlin package is `io.github.corneloaie.admob`, and the Gradle `group` is `io.github.corneloaie`, which also names
the SwiftPM cinterop package (`swiftPMImport.io.github.corneloaie.admob`) that the iOS sources import.

## The SDK versions

- **Android:** `play-services-ads` and `user-messaging-platform` are exact versions, which apps can raise.
- **iOS:** GoogleMobileAds and UMP are SwiftPM lower bounds (`from`): an app resolves the newest release of the same
  major. The floors, 13.3.0 and 3.0.0, are what InfoTraffic pins in Xcode, so an app linking either package itself
  still resolves one copy. The library must only call API that both floors have. The `floor` job of CI builds and tests
  with `-Padmob.pinIosSdkFloor`, which resolves exactly the floors, and fails on anything newer (UMP's `consentSyncID`
  is one such API, so it isn't offered). Raise a floor only together with the apps that pin it.
- **Changing an iOS version, or switching between the floor and the newest SDKs:** start from a clean build. Run
  `rm -rf .swiftpm-locks build admob/build`. SwiftPM keeps a version its lock file (`.swiftpm-locks`, not committed)
  still allows. Gradle doesn't rerun cinterop or relink when only the resolved SDK changed. And a stale
  `build/kotlin/swiftPMXcodeDumps` fails the build (KT-88106). CI always starts clean.
- **Mapping SDK values:** map every SDK constant explicitly, on each platform. Android and iOS number some of them
  differently: consent status, AdChoices position, and the load error codes (no fill is 3 on Android, 1 on iOS).
- **Publishing tooling:** the publish plugin (`publish` in the catalog) is only exercised by a Publish run. Bump it
  only with a dry run on its branch: `gh workflow run publish.yml --ref <branch>`.

## The public API

`./gradlew :admob:checkKotlinAbi` compares the public API with `admob/api/`, and CI runs it. After an intended change,
run `./gradlew :admob:updateKotlinAbi` and commit the new dump. Its diff is the API change to review. While the version
is 0.x, an incompatible change bumps the minor version.

## Releasing

1. Set `version` in `gradle.properties` (semantic versioning; `0.x` while the API is still settling). Commit it to
   `main`.
2. Run the `Publish` workflow by hand on `main`: `gh workflow run publish.yml`. Run by hand, it is a dry run. It builds
   the publications on macOS, and `.github/scripts/publish/check_publications.py` checks:
   - the POMs and the Gradle module files;
   - the sources and javadoc jars;
   - the iOS cinterop klibs;
   - the SwiftPM metadata through which apps inherit the iOS SDKs.

   The `publications` artifact holds what would be uploaded. A version on Maven Central can never be replaced or
   deleted, so release only after this passes.
3. Create a GitHub release with the tag `v<version>`, for example
   `gh release create v0.1.0 --target main --title v0.1.0 --generate-notes`. The `Publish` workflow does the following:
   - checks the tag against `gradle.properties`;
   - builds and checks the publications again;
   - uploads them to the Central Portal as one deployment, which is released automatically.

   A failure anywhere uploads nothing.
4. The artifact shows up at <https://repo1.maven.org/maven2/io/github/corneloaie/admob/> within minutes, and on
   central.sonatype.com search later.

To check the publications locally:

```bash
./gradlew :admob:publishToMavenLocal
python3 .github/scripts/publish/check_publications.py ~/.m2/repository
```

Publishing into Maven local is never signed (see the root build file); only `publishToMavenCentral` signs.

### One-time Maven Central setup

The namespace `io.github.corneloaie` is verified on the Central Portal already, through the firebase-multiplatform
releases.

1. Sign in to [central.sonatype.com](https://central.sonatype.com) with the `corneloaie` GitHub account. Under Account,
   choose Generate User Token, and keep the page open.
2. Run `.github/scripts/publish/set-publishing-secrets.sh` in a terminal. It asks for the token's username and password.
   It then finds the signing key "Cornel Oaie (firebase-multiplatform release signing)" in your GnuPG keyring, the one
   the Firebase releases use, and asks for that key's passphrase. It republishes the key's public half on
   keyserver.ubuntu.com and keys.openpgp.org. Finally, it stores the four secrets the Publish workflow reads in this
   repository: `SONATYPEUSERNAME`, `SONATYPEPASSWORD`, `GPG_PRIVATE_KEY` and `PASSPHRASE`. No value is printed, and
   none leaves your machine except to GitHub.

## Building on a laptop

- **Requirements:** JDK 17 or 21, the Android SDK, and Xcode 26.2 or newer with the iOS platform.
- **Network:** the iOS simulator tests request Google's public test ads. A DNS blocker (AdGuard, Pi-hole) makes those
  requests time out, and the tests accept that. Pause the blocker to see a real fill.
