package io.github.corneloaie.admob

/** How the SDK and each mediation adapter started, by adapter class name (see [AdMob.initialize]). */
public data class InitializationStatus(val adapterStatuses: Map<String, AdapterStatus>)

/**
 * How one adapter started.
 *
 * @property description the SDK's explanation of [state].
 * @property latencyMillis how long the adapter took to start.
 */
public data class AdapterStatus(
    val state: AdapterState,
    val description: String,
    val latencyMillis: Long,
)

public enum class AdapterState {
    /** The adapter didn't start in time, or failed to: it is less likely to fill requests. */
    NOT_READY,

    /** The adapter is ready to fill requests. */
    READY,
}
