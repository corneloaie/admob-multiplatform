package io.github.corneloaie.admob

import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class OnceInitializerTest {
    @Test
    fun startsOnceAndHandsEveryCallerTheResult() = runTest {
        var starts = 0
        var complete: ((String) -> Unit)? = null
        val once = OnceInitializer<String> { onComplete ->
            starts++
            complete = onComplete
        }

        val first = async { once.await() }
        val second = async { once.await() }
        runCurrent()
        checkNotNull(complete)("ready")

        assertEquals("ready", first.await())
        assertEquals("ready", second.await())
        assertEquals("ready", once.await())
        assertEquals(1, starts)
    }

    @Test
    fun cancellingTheFirstCallerDoesNotCancelTheStart() = runTest {
        var complete: ((String) -> Unit)? = null
        val once = OnceInitializer<String> { complete = it }

        val first = launch { once.await() }
        runCurrent()
        first.cancel()
        val second = async { once.await() }
        runCurrent()
        checkNotNull(complete)("ready")

        assertEquals("ready", second.await())
    }
}
