package com.raj.codex.gateway
import com.raj.codex.providers.buildAdapter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
/** Single entry point. Nothing outside adapters knows a provider wire format. */
class ModelGateway(private val secretStore: com.raj.codex.security.SecretStore) {
    private val breakers = mutableMapOf<String, CircuitBreaker>()
    fun adapterFor(providerId: String): ProviderAdapter = buildAdapter(providerId, secretStore)
    suspend fun chat(providerId: String, req: ChatRequest): ChatResponse {
        val breaker = breakers.getOrPut(providerId) { CircuitBreaker() }
        if (breaker.isOpen) throw AltrexError.ProviderFailure("$providerId abhi band hai (circuit open), thodi der baad try karo.")
        return try {
            val r = adapterFor(providerId).chat(req)
            breaker.recordSuccess(); r
        } catch (e: AltrexError) {
            if (e is AltrexError.RateLimited || e is AltrexError.ProviderFailure) breaker.recordFailure()
            throw e
        }
    }
    fun stream(providerId: String, req: ChatRequest): Flow<StreamChunk> = flow {
        val adapter = adapterFor(providerId)
        adapter.stream(req).collect { emit(it) }
    }
    suspend fun health(providerId: String): ProviderHealth = try {
        adapterFor(providerId).healthCheck()
    } catch (e: Exception) { ProviderHealth(HealthStatus.OFFLINE, e.message ?: "Offline") }
}
