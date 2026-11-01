package io.github.corneloaie.admob

import com.google.android.gms.ads.MobileAds

public actual object AdMob {
    public actual val sdkVersion: String
        get() = MobileAds.getVersion().toString()
}
