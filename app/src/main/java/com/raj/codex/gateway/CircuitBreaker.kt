package com.raj.codex.gateway
/** Open after 3 consecutive failures, cool down 60s. Do not hammer a failing provider. */
class CircuitBreaker(private val maxFailures: Int = 3, private val cooldownMs: Long = 60_000) {
    private var failures = 0
    private var openedAtMs: Long = 0
    val isOpen: Boolean get() = failures >= maxFailures && System.currentTimeMillis() - openedAtMs < cooldownMs
    fun recordSuccess() { failures = 0 }
    fun recordFailure() { if (failures == 0 || failures < maxFailures) failures++; if (failures >= maxFailures) openedAtMs = System.currentTimeMillis() }
}
