package io.github.corneloaie.admob

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext

/**
 * The User Messaging Platform (UMP) SDK: the consent the app must gather before requesting ads where regulations
 * require it (GDPR in the EEA and UK, US state privacy laws).
 *
 * Google's order: [requestInfoUpdate] at every app launch, then [loadAndShowConsentFormIfRequired], then start the ads
 * SDK ([AdMob.initialize]) and request ads once [canRequestAds] is true. Offer a privacy settings entry point that
 * calls [showPrivacyOptionsForm] when [privacyOptionsRequirementStatus] is [PrivacyOptionsRequirementStatus.REQUIRED].
 */
public object AdConsent {
    private val controller by lazy { ConsentController(consentPlatform()) }

    /**
     * The consent state, published again by every call of this object. It changes as soon as [requestInfoUpdate] is
     * called, not only when it completes: UMP loads the consent given in an earlier session at once, so a returning
     * user's ads needn't wait for the network.
     */
    public val state: StateFlow<ConsentState> get() = controller.state

    /** The user's consent status, as the SDK holds it now. */
    public val consentStatus: ConsentStatus get() = controller.platform.consentStatus

    /**
     * Whether ads may be requested, as the SDK holds it now: consent was obtained or isn't required. It reads false
     * until [requestInfoUpdate] has been called in this session.
     */
    public val canRequestAds: Boolean get() = controller.platform.canRequestAds

    /** Whether the app must offer an entry point to [showPrivacyOptionsForm], as the SDK holds it now. */
    public val privacyOptionsRequirementStatus: PrivacyOptionsRequirementStatus
        get() = controller.platform.privacyOptionsRequirementStatus

    /** Whether a consent form is available to show. */
    public val isConsentFormAvailable: Boolean get() = controller.platform.isConsentFormAvailable

    /**
     * Asks UMP for the user's consent requirements, without showing anything. Call it at every app launch.
     *
     * @throws AdMobException when the update fails, e.g. offline. The state still holds the consent saved earlier,
     *   so [canRequestAds] may be true anyway.
     */
    public suspend fun requestInfoUpdate(
        host: AdHost,
        parameters: ConsentRequestParameters = ConsentRequestParameters(),
    ): Unit = withContext(Dispatchers.Main.immediate) { controller.requestInfoUpdate(host, parameters) }

    /**
     * Loads the consent form and shows it over [host] if consent is required and missing, and returns once the user
     * has answered. Returns at once when no form is needed. Call it after [requestInfoUpdate].
     *
     * @throws AdMobException when the form can't be loaded or shown.
     */
    public suspend fun loadAndShowConsentFormIfRequired(host: AdHost): Unit =
        withContext(Dispatchers.Main.immediate) { controller.loadAndShowConsentFormIfRequired(host) }

    /**
     * Shows the privacy options form over [host], where the user can change their choices, and returns once it closes.
     *
     * @throws AdMobException when the form can't be shown.
     */
    public suspend fun showPrivacyOptionsForm(host: AdHost): Unit =
        withContext(Dispatchers.Main.immediate) { controller.showPrivacyOptionsForm(host) }

    /**
     * Forgets the consent given on this device, for testing: the next [requestInfoUpdate] and
     * [loadAndShowConsentFormIfRequired] behave as on a fresh install. Ads can't be requested until the user answers.
     */
    public fun reset() {
        controller.reset()
    }
}

/** A snapshot of [AdConsent]'s state. */
public data class ConsentState(
    val consentStatus: ConsentStatus,
    val canRequestAds: Boolean,
    val privacyOptionsRequirementStatus: PrivacyOptionsRequirementStatus,
)

public enum class ConsentStatus {
    /** Not known yet: [AdConsent.requestInfoUpdate] hasn't been called in this session. */
    UNKNOWN,

    /** Consent is required and not obtained yet. */
    REQUIRED,

    /** No consent is required, e.g. outside a regulated region. */
    NOT_REQUIRED,

    /** The user answered the consent form (which choices they made, the SDK doesn't say). */
    OBTAINED,
}

public enum class PrivacyOptionsRequirementStatus {
    UNKNOWN,

    /** The app must offer an entry point to the privacy options form. */
    REQUIRED,

    NOT_REQUIRED,
}

/**
 * Parameters of [AdConsent.requestInfoUpdate].
 *
 * @property tagForUnderAgeOfConsent whether the user is under the age of consent. UMP then shows no form, and the ads
 *   are non-personalized.
 * @property consentSyncId an id for the user, to share their consent choices between the apps that send the same id
 *   (enable consent syncing for the apps in the Privacy & messaging settings). It's a UUID or 22 to 150 characters of
 *   `[0-9a-zA-Z+.=/_\-$,{}]`; UMP ignores any other value, with a warning in the log.
 * @property debugSettings to test the consent flow: production builds leave it null.
 */
public data class ConsentRequestParameters(
    val tagForUnderAgeOfConsent: Boolean = false,
    val consentSyncId: String? = null,
    val debugSettings: ConsentDebugSettings? = null,
)

/**
 * Makes UMP behave as in another region, on test devices only.
 *
 * @property testDeviceHashedIds the test devices: UMP logs a device's hashed id the first time it requests an update.
 *   Android emulators and the iOS simulator are test devices already.
 */
public data class ConsentDebugSettings(
    val geography: DebugGeography = DebugGeography.DISABLED,
    val testDeviceHashedIds: List<String> = emptyList(),
)

/** The region a test device appears to be in. */
public enum class DebugGeography {
    /** Its real region. */
    DISABLED,

    /** The EEA or the UK, where GDPR applies. */
    EEA,

    /** A US state with a privacy law UMP supports. */
    REGULATED_US_STATE,

    /** A region where no supported regulation applies. */
    OTHER,
}
