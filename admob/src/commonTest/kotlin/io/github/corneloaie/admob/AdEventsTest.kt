package io.github.corneloaie.admob

import kotlinx.coroutines.async
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class AdEventsTest {
    private val paid = AdEvent.Paid(AdValue(1_500_000, "USD", AdValuePrecision.PRECISE))

    @Test
    fun replaysWhatHappenedBeforeCollectionStarted() = runTest {
        val events = AdEvents()
        events.emit(AdEvent.Impression)
        events.emit(paid)
        events.close()

        assertEquals(listOf(AdEvent.Impression, paid), events.flow.toList())
    }

    @Test
    fun everyCollectorGetsEveryEventAndCompletesOnClose() = runTest {
        val events = AdEvents()
        events.emit(AdEvent.Impression)
        val early = async { events.flow.toList() }
        runCurrent()
        events.emit(AdEvent.Click)
        events.close()

        assertEquals(listOf(AdEvent.Impression, AdEvent.Click), early.await())
        assertEquals(listOf(AdEvent.Impression, AdEvent.Click), events.flow.toList())
    }

    @Test
    fun dropsEventsAfterClose() = runTest {
        val events = AdEvents()
        events.close()
        events.emit(AdEvent.Click)
        events.close()

        assertEquals(emptyList(), events.flow.toList())
    }
}
