package com.raj.codex.gateway
data class ModelInfo(val providerId: String, val modelId: String, val displayName: String, val contextSize: Int, val toolSupport: Boolean, val vision: Boolean, val streaming: Boolean, val free: Boolean, var health: HealthStatus = HealthStatus.UNKNOWN)
/** Registry: known models only. Discovery (listModels) adds to it at runtime; never invent a model. */
object ModelRegistry {
    val models: List<ModelInfo> = listOf(
        ModelInfo("gemini", "gemini-2.5-flash", "Gemini 2.5 Flash", 1_000_000, true, true, true, false),
        ModelInfo("openrouter", "openai/gpt-oss-120b:free", "GPT-OSS 120B (free)", 131_072, true, false, true, true),
        ModelInfo("groq", "llama-3.3-70b-versatile", "Llama 3.3 70B", 131_072, true, false, true, false),
        ModelInfo("nvidia", "meta/llama-3.3-70b-instruct", "Llama 3.3 70B NIM", 131_072, true, false, true, false),
        ModelInfo("ollama", "qwen2.5-coder:7b", "Qwen2.5 Coder 7B (local)", 32_768, false, false, true, true),
        ModelInfo("crax-gpt", "crax-gpt-1", "crax-gpt (free gateway)", 128_000, true, false, true, true),
        ModelInfo("custom", "custom-model", "Custom OpenAI-compatible", 128_000, true, false, true, false)
    )
    fun find(providerId: String, modelId: String): ModelInfo? = models.firstOrNull { it.providerId == providerId && it.modelId == modelId }
}
