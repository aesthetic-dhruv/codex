package com.raj.codex.router
import com.raj.codex.gateway.HealthStatus
import com.raj.codex.gateway.ModelInfo
import com.raj.codex.gateway.ModelRegistry
enum class RoutingMode { AUTO, FAST, POWERFUL, FREE_ONLY, LOCAL_ONLY, CUSTOM }
data class RoutingDecision(val providerId: String, val modelId: String, val reason: String, val fallbacks: List<Pair<String, String>>)
/** Pure decision: healthy + eligible first. Role is never tied to one model. */
object ModelRouter {
    fun decide(mode: RoutingMode, needTools: Boolean, needVision: Boolean, custom: Pair<String, String>? = null, health: Map<String, HealthStatus> = emptyMap()): RoutingDecision {
        if (mode == RoutingMode.CUSTOM && custom != null) {
            val info = ModelRegistry.find(custom.first, custom.second) ?: throw IllegalArgumentException("Model ${custom.second} registry me nahi hai — galat provider ko request nahi bhejenge.")
            return RoutingDecision(info.providerId, info.modelId, "Custom choice", emptyList())
        }
        var pool = ModelRegistry.models.filter { (!needTools || it.toolSupport) && (!needVision || it.vision) }
        when (mode) {
            RoutingMode.FREE_ONLY -> pool = pool.filter { it.free }
            RoutingMode.LOCAL_ONLY -> pool = pool.filter { it.providerId == "ollama" }
            RoutingMode.FAST -> pool = pool.sortedBy { it.contextSize }
            RoutingMode.POWERFUL -> pool = pool.sortedByDescending { it.contextSize }
            else -> {}
        }
        pool = pool.filter { health[it.providerId] != HealthStatus.AUTH_ERROR && health[it.providerId] != HealthStatus.OFFLINE }
        val first = pool.firstOrNull() ?: throw IllegalStateException("Koi healthy eligible model nahi mila. Provider key ya health check karo.")
        val fallbacks = pool.drop(1).take(2).map { it.providerId to it.modelId }
        return RoutingDecision(first.providerId, first.modelId, "Mode $mode, healthy eligible model chuna", fallbacks)
    }
}
