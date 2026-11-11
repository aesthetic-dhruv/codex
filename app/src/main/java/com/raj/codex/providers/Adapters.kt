package com.raj.codex.providers
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.raj.codex.gateway.*
import com.raj.codex.security.SecretStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit
private val gson = Gson()
private val http = OkHttpClient.Builder().connectTimeout(30, TimeUnit.SECONDS).readTimeout(120, TimeUnit.SECONDS).build()
private fun postJson(url: String, key: String?, body: String): String {
    val reqB = Request.Builder().url(url).post(body.toRequestBody("application/json".toMediaType()))
    if (!key.isNullOrBlank()) reqB.addHeader("Authorization", "Bearer $key")
    val resp = http.newCall(reqB.build()).execute()
    val text = resp.body?.string() ?: ""
    if (!resp.isSuccessful) throw ErrorNormalizer.fromHttp(resp.code, text)
    return text
}
/** One OpenAI-compatible client for OpenRouter, Groq, NVIDIA, Ollama, custom endpoints (incl. ngrok tunnels). ngrok is NOT a provider. */
class OpenAiCompatibleAdapter(override val providerId: String, override val displayName: String, private val baseUrl: String, private val store: SecretStore, private val defaultModel: String) : ProviderAdapter {
    override suspend fun listModels(): List<String> {
        val text = try { val r = Request.Builder().url("$baseUrl/models").apply { store.getKey(providerId)?.let { addHeader("Authorization", "Bearer $it") } }.build(); http.newCall(r).execute().body?.string() ?: "" } catch (e: Exception) { throw AltrexError.Network() }
        return try { gson.fromJson(text, JsonObject::class.java).getAsJsonArray("data").map { it.asJsonObject.get("id").asString } } catch (e: Exception) { listOf(defaultModel) }
    }
    override suspend fun healthCheck(): ProviderHealth {
        return try { val m = listModels(); if (m.isNotEmpty()) ProviderHealth(HealthStatus.HEALTHY, "${m.size} models mile") else ProviderHealth(HealthStatus.DEGRADED, "Model list khali") } catch (e: AltrexError) { ProviderHealth(if (e is AltrexError.Auth) HealthStatus.AUTH_ERROR else if (e is AltrexError.RateLimited) HealthStatus.RATE_LIMITED else HealthStatus.OFFLINE, e.message ?: "") } catch (e: Exception) { ProviderHealth(HealthStatus.OFFLINE, e.message ?: "") }
    }
    override suspend fun chat(req: ChatRequest): ChatResponse {
        val key = store.getKey(providerId) ?: if (providerId == "ollama") "" else throw AltrexError.Auth()
        val body = gson.toJson(mapOf("model" to req.model, "messages" to req.messages.map { mapOf("role" to it.role, "content" to it.content) }, "temperature" to req.temperature, "max_tokens" to req.maxTokens))
        val text = postJson("$baseUrl/chat/completions", key, body)
        val obj = gson.fromJson(text, JsonObject::class.java)
        val content = obj.getAsJsonArray("choices").get(0).asJsonObject.getAsJsonObject("message").get("content").asString
        return ChatResponse(content, req.model)
    }
    override fun stream(req: ChatRequest): Flow<StreamChunk> = flow { emit(StreamChunk(chat(req).text, true)) }
    override fun getCapabilities(model: String) = ModelCapability(true, false, true, false, false, 131072)
    override suspend fun validateCredentials(): Boolean = try { listModels(); true } catch (e: Exception) { false }
}
class GeminiAdapter(private val store: SecretStore) : ProviderAdapter {
    override val providerId = "gemini"; override val displayName = "Google Gemini"
    override suspend fun listModels(): List<String> = listOf("gemini-2.5-flash")
    override suspend fun healthCheck(): ProviderHealth = if (store.hasKey("gemini")) ProviderHealth(HealthStatus.HEALTHY, "Key saved hai") else ProviderHealth(HealthStatus.AUTH_ERROR, "Gemini key nahi hai")
    override suspend fun chat(req: ChatRequest): ChatResponse {
        val key = store.getKey("gemini") ?: throw AltrexError.Auth()
        val parts = req.messages.joinToString("\n") { "${it.role}: ${it.content}" }
        val body = gson.toJson(mapOf("contents" to listOf(mapOf("parts" to listOf(mapOf("text" to parts))))))
        val url = "https://generativelanguage.googleapis.com/v1beta/models/${req.model}:generateContent?key=$key"
        val text = postJson(url, null, body)
        val obj = gson.fromJson(text, JsonObject::class.java)
        val content = obj.getAsJsonArray("candidates").get(0).asJsonObject.getAsJsonObject("content").getAsJsonArray("parts").get(0).asJsonObject.get("text").asString
        return ChatResponse(content, req.model)
    }
    override fun stream(req: ChatRequest): Flow<StreamChunk> = flow { emit(StreamChunk(chat(req).text, true)) }
    override fun getCapabilities(model: String) = ModelCapability(true, true, true, true, false, 1_000_000)
    override suspend fun validateCredentials(): Boolean = store.hasKey("gemini")
}
fun buildAdapter(providerId: String, store: SecretStore): ProviderAdapter = when (providerId) {
    "gemini" -> GeminiAdapter(store)
    "openrouter" -> OpenAiCompatibleAdapter("openrouter", "OpenRouter", "https://openrouter.ai/api/v1", store, "openai/gpt-oss-120b:free")
    "groq" -> OpenAiCompatibleAdapter("groq", "Groq", "https://api.groq.com/openai/v1", store, "llama-3.3-70b-versatile")
    "nvidia" -> OpenAiCompatibleAdapter("nvidia", "NVIDIA NIM", "https://integrate.api.nvidia.com/v1", store, "meta/llama-3.3-70b-instruct")
    "ollama" -> OpenAiCompatibleAdapter("ollama", "Ollama (local)", store.getBaseUrl("ollama") ?: "http://10.0.2.2:11434/v1", store, "qwen2.5-coder:7b")
    "crax-gpt" -> OpenAiCompatibleAdapter("crax-gpt", "crax-gpt", "https://api.crax-gpt.example/v1", store, "crax-gpt-1")
    "custom" -> OpenAiCompatibleAdapter("custom", "Custom OpenAI-compatible", store.getBaseUrl("custom") ?: "https://example.com/v1", store, "custom-model")
    else -> throw AltrexError.NotFound("Provider $providerId nahi hai")
}
