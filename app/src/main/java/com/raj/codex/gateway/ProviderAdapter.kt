package com.raj.codex.gateway
import kotlinx.coroutines.flow.Flow
data class ChatMessage(val role: String, val content: String)
data class ChatRequest(val messages: List<ChatMessage>, val model: String, val maxTokens: Int = 4096, val temperature: Double = 0.2, val tools: List<String> = emptyList())
data class ChatResponse(val text: String, val model: String, val inputTokens: Int? = null, val outputTokens: Int? = null)
data class StreamChunk(val text: String, val done: Boolean = false)
data class ProviderHealth(val status: HealthStatus, val detail: String, val checkedAtMs: Long = System.currentTimeMillis())
enum class HealthStatus { HEALTHY, DEGRADED, RATE_LIMITED, AUTH_ERROR, OFFLINE, UNSUPPORTED, UNKNOWN }
data class ModelCapability(val toolSupport: Boolean, val vision: Boolean, val streaming: Boolean, val structuredOutput: Boolean, val reasoning: Boolean, val contextSize: Int)
interface ProviderAdapter {
    val providerId: String
    val displayName: String
    suspend fun listModels(): List<String>
    suspend fun healthCheck(): ProviderHealth
    suspend fun chat(req: ChatRequest): ChatResponse
    fun stream(req: ChatRequest): Flow<StreamChunk>
    fun getCapabilities(model: String): ModelCapability
    suspend fun validateCredentials(): Boolean
    fun normalizeError(status: Int, body: String): AltrexError = ErrorNormalizer.fromHttp(status, body)
}
