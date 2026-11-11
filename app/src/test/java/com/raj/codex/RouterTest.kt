package com.raj.codex
import com.raj.codex.gateway.HealthStatus
import com.raj.codex.router.ModelRouter
import com.raj.codex.router.RoutingMode
import org.junit.Assert.*
import org.junit.Test
class RouterTest {
    @Test fun freeOnlyExcludesPaid() { val d = ModelRouter.decide(RoutingMode.FREE_ONLY, false, false); val info = com.raj.codex.gateway.ModelRegistry.find(d.providerId, d.modelId)!!; assertTrue(info.free) }
    @Test fun localOnlyIsOllama() { val d = ModelRouter.decide(RoutingMode.LOCAL_ONLY, false, false); assertEquals("ollama", d.providerId) }
    @Test fun customWrongProviderFails() { try { ModelRouter.decide(RoutingMode.CUSTOM, false, false, "gemini" to "llama-3.3-70b-versatile"); fail("should throw") } catch (e: IllegalArgumentException) { assertTrue(e.message!!.contains("galat provider")) } }
    @Test fun unhealthyProviderSkipped() { val d = ModelRouter.decide(RoutingMode.AUTO, false, false, health = mapOf("gemini" to HealthStatus.AUTH_ERROR)); assertNotEquals("gemini", d.providerId) }
}
