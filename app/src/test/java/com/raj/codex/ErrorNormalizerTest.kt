package com.raj.codex
import com.raj.codex.gateway.AltrexError
import com.raj.codex.gateway.ErrorNormalizer
import org.junit.Assert.*
import org.junit.Test
class ErrorNormalizerTest {
    @Test fun maps401toAuth() { assertTrue(ErrorNormalizer.fromHttp(401, "") is AltrexError.Auth) }
    @Test fun maps404toNotFound() { assertTrue(ErrorNormalizer.fromHttp(404, "") is AltrexError.NotFound) }
    @Test fun maps413toTooLarge() { assertTrue(ErrorNormalizer.fromHttp(413, "") is AltrexError.TooLarge) }
    @Test fun maps429toRateLimited() { assertTrue(ErrorNormalizer.fromHttp(429, "") is AltrexError.RateLimited) }
    @Test fun maps500toProviderFailure() { assertTrue(ErrorNormalizer.fromHttp(500, "") is AltrexError.ProviderFailure) }
    @Test fun authMessageHasNoRawKey() { val e = ErrorNormalizer.fromHttp(401, "Bearer sk-secret123"); assertFalse(e.message!!.contains("sk-secret123")) }
}
