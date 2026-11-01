package io.github.corneloaie.admob

/** Spike entry point: proves each platform reaches its native Google Mobile Ads SDK from Kotlin. */
public expect object AdMob {
    /** The linked Google Mobile Ads SDK version, as the SDK reports it. */
    public val sdkVersion: String
}
