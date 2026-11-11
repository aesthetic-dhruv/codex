package com.raj.codex.gateway
/** Normalised error. Never show raw provider text to the user. */
sealed class AltrexError(val code: String, message: String) : Exception(message) {
    class InvalidRequest(m: String) : AltrexError("INVALID_REQUEST", m)
    class Auth(m: String = "API key galat hai ya expired hai. Settings me nayi key daalo.") : AltrexError("AUTH_ERROR", m)
    class Forbidden(m: String = "Is model ya provider tak access nahi hai.") : AltrexError("FORBIDDEN", m)
    class NotFound(m: String = "Model ya endpoint nahi mila.") : AltrexError("NOT_FOUND", m)
    class Timeout(m: String = "Provider ne time par jawab nahi diya.") : AltrexError("TIMEOUT", m)
    class TooLarge(m: String = "Request bahut badi hai, context kam karo.") : AltrexError("TOO_LARGE", m)
    class RateLimited(val retryAfterMs: Long = 0, m: String = "Rate limit lag gaya, thodi der ruk ke dobara try karo.") : AltrexError("RATE_LIMITED", m)
    class ProviderFailure(m: String = "Provider server me problem hai.") : AltrexError("PROVIDER_FAILURE", m)
    class Network(m: String = "Internet nahi mil raha.") : AltrexError("NETWORK", m)
    class ToolUnsupported(m: String = "Ye model tools support nahi karta.") : AltrexError("TOOL_UNSUPPORTED", m)
    class Cancelled : AltrexError("CANCELLED", "Task cancel kar diya gaya.")
}
object ErrorNormalizer {
    fun fromHttp(status: Int, body: String): AltrexError = when (status) {
        400 -> AltrexError.InvalidRequest("Galat request: $body".take(300))
        401 -> AltrexError.Auth()
        403 -> AltrexError.Forbidden()
        404 -> AltrexError.NotFound()
        408 -> AltrexError.Timeout()
        413 -> AltrexError.TooLarge()
        429 -> AltrexError.RateLimited()
        500, 502, 503, 504 -> AltrexError.ProviderFailure("Provider error $status")
        else -> AltrexError.ProviderFailure("Provider error $status")
    }
}
