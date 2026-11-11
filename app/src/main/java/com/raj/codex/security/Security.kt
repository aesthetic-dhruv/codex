package com.raj.codex.security
import android.content.Context
/** Keys live ONLY in app-private storage. Never logged, never in prompts, never returned to UI. */
class SecretStore(context: Context) {
    private val prefs = context.getSharedPreferences("codex_secrets", Context.MODE_PRIVATE)
    fun saveKey(providerId: String, key: String) { prefs.edit().putString("key_$providerId", key).apply() }
    fun getKey(providerId: String): String? = prefs.getString("key_$providerId", null)
    fun saveBaseUrl(providerId: String, url: String) { prefs.edit().putString("url_$providerId", url).apply() }
    fun getBaseUrl(providerId: String): String? = prefs.getString("url_$providerId", null)
    fun hasKey(providerId: String): Boolean = !getKey(providerId).isNullOrBlank()
}
enum class Risk { LOW, MEDIUM, HIGH }
object PermissionPolicy {
    fun riskOf(command: String): Risk {
        val c = command.trim().lowercase()
        if (c.contains("rm -rf") || c.contains("git push") || c.contains("format") || c.startsWith("sudo")) return Risk.HIGH
        if (c.startsWith("npm") || c.startsWith("npx") || c.startsWith("pip install") || c.contains("delete")) return Risk.MEDIUM
        return Risk.LOW
    }
    /** HIGH commands are never run automatically. MEDIUM needs user approval. */
    fun allowedWithoutApproval(risk: Risk): Boolean = risk == Risk.LOW
}
object PathGuard {
    fun isSafe(projectRoot: String, path: String): Boolean {
        val root = java.io.File(projectRoot).canonicalPath
        val target = java.io.File(projectRoot, path).canonicalPath
        return target.startsWith(root) && !target.contains("..")
    }
}
