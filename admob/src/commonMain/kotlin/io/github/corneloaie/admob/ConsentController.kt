package io.github.corneloaie.admob

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/** The UMP SDK of one platform. Each call reports its outcome to `onComplete` exactly once, on the main thread. */
internal interface ConsentPlatform<in Host> {
    val consentStatus: ConsentStatus
    val canRequestAds: Boolean
    val privacyOptionsRequirementStatus: PrivacyOptionsRequirementStatus
    val isConsentFormAvailable: Boolean

    fun requestInfoUpdate(host: Host, parameters: ConsentRequestParameters, onComplete: (AdError?) -> Unit)

    fun loadAndShowConsentFormIfRequired(host: Host, onComplete: (AdError?) -> Unit)

    fun showPrivacyOptionsForm(host: Host, onComplete: (AdError?) -> Unit)

    fun reset()
}

internal expect fun consentPlatform(): ConsentPlatform<AdHost>

/** [AdConsent] over a [ConsentPlatform]: turns its callbacks into suspending calls and keeps [state] current. */
internal class ConsentController<Host>(val platform: ConsentPlatform<Host>) {
    private val mutableState = MutableStateFlow(platform.readState())
    val state: StateFlow<ConsentState> = mutableState.asStateFlow()

    suspend fun requestInfoUpdate(host: Host, parameters: ConsentRequestParameters) = await { onComplete ->
        platform.requestInfoUpdate(host, parameters, onComplete)
        // UMP loads the consent saved in an earlier session as soon as the update is requested: publish it now, so a
        // returning user's ads don't wait for the network round trip.
        refresh()
    }

    suspend fun loadAndShowConsentFormIfRequired(host: Host) = await { onComplete ->
        platform.loadAndShowConsentFormIfRequired(host, onComplete)
    }

    suspend fun showPrivacyOptionsForm(host: Host) = await { onComplete ->
        platform.showPrivacyOptionsForm(host, onComplete)
    }

    fun reset() {
        platform.reset()
        refresh()
    }

    private fun refresh() {
        mutableState.value = platform.readState()
    }

    private suspend fun await(call: (onComplete: (AdError?) -> Unit) -> Unit) {
        val error = suspendCancellableCoroutine { continuation ->
            call { error ->
                // Before resuming, and also when the caller has gone: the state is everyone's.
                refresh()
                continuation.resume(error)
            }
        }
        if (error != null) throw AdMobException(error)
    }
}

private fun ConsentPlatform<*>.readState() = ConsentState(consentStatus, canRequestAds, privacyOptionsRequirementStatus)
