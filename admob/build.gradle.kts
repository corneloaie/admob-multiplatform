import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.TestExecutable

plugins {
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.kotlin.multiplatform)
    `maven-publish`
}

group = "io.github.corneloaie"
version = "0.1.0-SNAPSHOT"

kotlin {
    explicitApi()

    android {
        namespace = "io.github.corneloaie.admob"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    listOf(iosArm64(), iosSimulatorArm64()).forEach { target ->
        // GMA reads GADApplicationIdentifier from the main bundle's Info.plist; a test executable has no bundle, so
        // embed one (Google's sample app id) the way command-line tools carry theirs.
        target.binaries.withType<TestExecutable>().configureEach {
            linkerOpts("-sectcreate", "__TEXT", "__info_plist", file("src/iosTest/Info.plist").absolutePath)
        }
    }

    // The Google Mobile Ads + UMP iOS SDKs are Obj-C binary frameworks shipped as Swift packages. Kotlin's SwiftPM
    // import resolves them, runs cinterop on the listed Clang modules, publishes the dependency as Maven metadata
    // (consumers inherit it, as with the Firebase lib) and links them into Kotlin/Native test executables.
    swiftPMDependencies {
        discoverClangModulesImplicitly = false
        iosMinimumDeploymentTarget.set(libs.versions.ios.deploymentTarget.get())
        swiftPackage(
            url = url("https://github.com/googleads/swift-package-manager-google-mobile-ads.git"),
            version = from(libs.versions.google.mobile.ads.ios.get()),
            products = listOf(product("GoogleMobileAds")),
            importedClangModules = listOf("GoogleMobileAds"),
        )
        swiftPackage(
            url = url("https://github.com/googleads/swift-package-manager-google-user-messaging-platform.git"),
            version = from(libs.versions.google.ump.ios.get()),
            products = listOf(product("GoogleUserMessagingPlatform")),
            importedClangModules = listOf("UserMessagingPlatform"),
        )
    }

    sourceSets {
        all {
            if (name.startsWith("ios")) {
                languageSettings.optIn("kotlinx.cinterop.ExperimentalForeignApi")
                languageSettings.optIn("kotlinx.cinterop.BetaInteropApi")
            }
        }
        androidMain.dependencies {
            api(libs.play.services.ads)
            api(libs.user.messaging.platform)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

// Spike: publish to a repo inside build/ to inspect the Gradle module metadata without touching ~/.m2.
publishing {
    repositories {
        maven {
            name = "spike"
            url = uri(layout.buildDirectory.dir("spike-repo"))
        }
    }
}
