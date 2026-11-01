package io.github.corneloaie.admob

import kotlinx.coroutines.async
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull

class FullScreenAdSessionTest {
    private val value = AdValue(20_000, "EUR", AdValuePrecision.ESTIMATED)
    private val reward = RewardItem("coins", 10)

    @Test
    fun showReturnsTheRewardOnceTheAdIsDismissed() = runTest {
        val session = FullScreenAdSession()
        var presented = 0
        val shown = async { session.show { presented++ } }
        runCurrent()
        session.onShown()
        session.onImpression()
        session.onPaid(value)
        session.onRewardEarned(reward)
        runCurrent()
        assertFalse(shown.isCompleted)

        session.onDismissed()

        assertEquals(reward, shown.await())
        assertEquals(1, presented)
        assertEquals(
            listOf(
                AdEvent.Shown,
                AdEvent.Impression,
                AdEvent.Paid(value),
                AdEvent.RewardEarned(reward),
                AdEvent.Dismissed,
            ),
            session.events.flow.toList(),
        )
    }

    @Test
    fun showReturnsNullWithoutAReward() = runTest {
        val session = FullScreenAdSession()
        val shown = async { session.show {} }
        runCurrent()
        session.onShown()
        session.onClick()
        session.onDismissed()

        assertNull(shown.await())
        assertEquals(listOf(AdEvent.Shown, AdEvent.Click, AdEvent.Dismissed), session.events.flow.toList())
    }

    @Test
    fun showThrowsWhenTheAdFailsToShow() = runTest {
        val error = AdError(AdErrorCode.ALREADY_USED, platformCode = 1, domain = "test", message = "used")
        val session = FullScreenAdSession()
        val shown = async { runCatching { session.show {} } }
        runCurrent()
        session.onFailedToShow(error)

        val thrown = assertFailsWith<AdMobException> { shown.await().getOrThrow() }
        assertEquals(error, thrown.error)
        assertEquals(listOf(AdEvent.FailedToShow(error)), session.events.flow.toList())
    }

    @Test
    fun aSecondShowWhileShowingIsRejected() = runTest {
        val session = FullScreenAdSession()
        val first = async { session.show {} }
        runCurrent()

        assertFailsWith<IllegalStateException> { session.show {} }

        session.onDismissed()
        assertNull(first.await())
    }
}
