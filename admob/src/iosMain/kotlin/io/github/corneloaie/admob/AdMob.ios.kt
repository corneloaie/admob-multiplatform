package io.github.corneloaie.admob

import swiftPMImport.io.github.corneloaie.admob.GADGetStringFromVersionNumber
import swiftPMImport.io.github.corneloaie.admob.GADMobileAds

public actual object AdMob {
    public actual val sdkVersion: String
        get() = GADGetStringFromVersionNumber(GADMobileAds.sharedInstance.versionNumber)
}
