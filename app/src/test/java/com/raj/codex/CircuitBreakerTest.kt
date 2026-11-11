package com.raj.codex
import com.raj.codex.gateway.CircuitBreaker
import org.junit.Assert.*
import org.junit.Test
class CircuitBreakerTest {
    @Test fun opensAfter3Failures() { val cb = CircuitBreaker(); repeat(3) { cb.recordFailure() }; assertTrue(cb.isOpen) }
    @Test fun successResets() { val cb = CircuitBreaker(); repeat(2) { cb.recordFailure() }; cb.recordSuccess(); assertFalse(cb.isOpen) }
    @Test fun twoFailuresNotOpen() { val cb = CircuitBreaker(); repeat(2) { cb.recordFailure() }; assertFalse(cb.isOpen) }
}
