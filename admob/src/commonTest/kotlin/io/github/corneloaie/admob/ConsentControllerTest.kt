package io.github.corneloaie.admob

import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ConsentControllerTest {
    private val platform = FakeConsentPlatform()
    private val controller = ConsentController(platform)

    @Test
    fun publishesTheSavedConsentAsSoonAsTheUpdateIsRequested() = runTest {
        // UMP loads the consent of an earlier session when the update is called, before its network round trip.
        platform.onRequestInfoUpdate = {
            platform.consentStatus = ConsentStatus.OBTAINED
            platform.canRequestAds = true
        }

        val update = launch { controller.requestInfoUpdate("activity", ConsentRequestParameters()) }
        runCurrent()

        assertTrue(controller.state.value.canRequestAds)
        assertFalse(update.isCompleted)
        platform.complete()
        update.join()
        assertEquals(listOf("update activity"), platform.calls)
    }

    @Test
    fun publishesTheStateTheUpdateEndsWith() = runTest {
        val update = launch { controller.requestInfoUpdate("activity", ConsentRequestParameters()) }
        runCurrent()
        platform.consentStatus = ConsentStatus.REQUIRED
        platform.privacyOptionsRequirementStatus = PrivacyOptionsRequirementStatus.REQUIRED
        platform.complete()
        update.join()

        assertEquals(
            ConsentState(ConsentStatus.REQUIRED, false, PrivacyOptionsRequirementStatus.REQUIRED),
            controller.state.value,
        )
    }

    @Test
    fun aFailedUpdatePublishesTheStateThenThrows() = runTest {
        val error = AdError(AdErrorCode.NETWORK_ERROR, platformCode = 2, domain = "test", message = "offline")
        val update = async { runCatching { controller.requestInfoUpdate("activity", ConsentRequestParameters()) } }
        runCurrent()
        platform.canRequestAds = true
        platform.complete(error)

        val thrown = assertFailsWith<AdMobException> { update.await().getOrThrow() }
        assertEquals(error, thrown.error)
        assertTrue(controller.state.value.canRequestAds)
    }

    @Test
    fun publishesTheOutcomeEvenWhenTheCallerIsGone() = runTest {
        val form = launch { controller.loadAndShowConsentFormIfRequired("activity") }
        runCurrent()
        form.cancel()
        platform.consentStatus = ConsentStatus.OBTAINED
        platform.canRequestAds = true
        platform.complete()

        assertEquals(ConsentStatus.OBTAINED, controller.state.value.consentStatus)
    }

    @Test
    fun formsGoToTheHostTheyAreGiven() = runTest {
        val form = launch { controller.loadAndShowConsentFormIfRequired("consent screen") }
        runCurrent()
        platform.complete()
        form.join()
        val privacy = launch { controller.showPrivacyOptionsForm("settings screen") }
        runCurrent()
        platform.complete()
        privacy.join()

        assertEquals(listOf("form consent screen", "privacy settings screen"), platform.calls)
    }

    @Test
    fun resetPublishesTheResetState() {
        platform.consentStatus = ConsentStatus.OBTAINED
        platform.canRequestAds = true
        controller.reset()

        assertEquals(
            ConsentState(ConsentStatus.UNKNOWN, false, PrivacyOptionsRequirementStatus.UNKNOWN),
            controller.state.value,
        )
        assertEquals(listOf("reset"), platform.calls)
    }
}

private class FakeConsentPlatform : ConsentPlatform<String> {
    override var consentStatus = ConsentStatus.UNKNOWN
    override var canRequestAds = false
    override var privacyOptionsRequirementStatus = PrivacyOptionsRequirementStatus.UNKNOWN
    override var isConsentFormAvailable = false

    val calls = mutableListOf<String>()
    var onRequestInfoUpdate: () -> Unit = {}
    private var pending: ((AdError?) -> Unit)? = null

    override fun requestInfoUpdate(host: String, parameters: ConsentRequestParameters, onComplete: (AdError?) -> Unit) {
        calls += "update $host"
        onRequestInfoUpdate()
        pending = onComplete
    }

    override fun loadAndShowConsentFormIfRequired(host: String, onComplete: (AdError?) -> Unit) {
        calls += "form $host"
        pending = onComplete
    }

    override fun showPrivacyOptionsForm(host: String, onComplete: (AdError?) -> Unit) {
        calls += "privacy $host"
        pending = onComplete
    }

    override fun reset() {
        calls += "reset"
        consentStatus = ConsentStatus.UNKNOWN
        canRequestAds = false
    }

    fun complete(error: AdError? = null) {
        val onComplete = checkNotNull(pending) { "no call is waiting" }
        pending = null
        onComplete(error)
    }
}
