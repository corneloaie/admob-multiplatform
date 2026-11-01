import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSetTree
import org.jetbrains.kotlin.gradle.plugin.mpp.TestExecutable

plugins {
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.publish)
}

// Also names the SwiftPM cinterop package after the group and the module: swiftPMImport.io.github.corneloaie.admob.
// The version comes from gradle.properties.
group = "io.github.corneloaie"

val repositoryUrl = "https://github.com/corneloaie/admob-multiplatform"

// -Padmob.pinIosSdkFloor resolves exactly the lowest GoogleMobileAds and UMP releases the library allows, to check that
// it only calls API those have. Run `rm -rf build/kotlin/swiftPMXcodeDumps` when switching (KT-88106).
val pinIosSdkFloor = providers.gradleProperty("admob.pinIosSdkFloor").isPresent

kotlin {
    explicitApi()

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    @OptIn(org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation::class)
    abiValidation {}

    android {
        namespace = "io.github.corneloaie.admob"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
        withHostTestBuilder { sourceSetTreeName = KotlinSourceSetTree.test.name }
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
        val gma = libs.versions.google.mobile.ads.ios.get()
        val ump = libs.versions.google.ump.ios.get()
        swiftPackage(
            url = url("https://github.com/googleads/swift-package-manager-google-mobile-ads.git"),
            version = if (pinIosSdkFloor) exact(gma) else from(gma),
            products = listOf(product("GoogleMobileAds")),
            importedClangModules = listOf("GoogleMobileAds"),
        )
        swiftPackage(
            url = url("https://github.com/googleads/swift-package-manager-google-user-messaging-platform.git"),
            version = if (pinIosSdkFloor) exact(ump) else from(ump),
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
            if (name.endsWith("Test")) {
                // runCurrent and the other kotlinx-coroutines-test scheduling controls.
                languageSettings.optIn("kotlinx.coroutines.ExperimentalCoroutinesApi")
            }
        }
        commonMain.dependencies {
            api(libs.kotlinx.coroutines.core)
        }
        androidMain.dependencies {
            api(libs.play.services.ads)
            api(libs.user.messaging.platform)
            implementation(libs.kotlinx.coroutines.android)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}

mavenPublishing {
    publishToMavenCentral(automaticRelease = true)
    signAllPublications()

    coordinates(groupId = group.toString(), artifactId = "admob", version = version.toString())

    pom {
        name.set("admob-multiplatform")
        description.set(
            "Kotlin Multiplatform Google Mobile Ads (AdMob) and UMP consent for Android and iOS: initialization, " +
                "consent, native, interstitial and rewarded ads from common code, with no Swift bridge.",
        )
        url.set(repositoryUrl)
        inceptionYear.set("2026")
        licenses {
            license {
                name.set("The Apache Software License, Version 2.0")
                url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                distribution.set("repo")
                comments.set("A business-friendly OSS license")
            }
        }
        developers {
            developer {
                id.set("corneloaie")
                name.set("Cornel Oaie")
                url.set("https://github.com/corneloaie")
            }
        }
        scm {
            url.set(repositoryUrl)
            connection.set("scm:git:$repositoryUrl.git")
            developerConnection.set("scm:git:ssh://git@github.com/corneloaie/admob-multiplatform.git")
            tag.set("HEAD")
        }
        issueManagement {
            system.set("GitHub Issues")
            url.set("$repositoryUrl/issues")
        }
    }
}
