package com.raj.codex.verification
import java.io.File
data class VerificationReport(val passed: Boolean, val checks: List<Pair<String, String>>, val summary: String)
/** Evidence-based only. Never report PASS unless the check actually ran. */
class VerificationEngine(private val root: File) {
    fun verify(): VerificationReport {
        val checks = mutableListOf<Pair<String, String>>()
        val index = File(root, "index.html")
        checks += "file-exists" to if (index.exists()) "PASS: index.html exists (${index.length()} bytes)" else "FAIL: index.html missing"
        val text = if (index.exists()) runCatching { index.readText() }.getOrDefault("") else ""
        checks += "html-valid" to if (text.contains("<html", ignoreCase = true) && text.contains("</html>", ignoreCase = true)) "PASS: html tags present" else "FAIL: html tags missing"
        checks += "not-empty" to if (text.length > 200) "PASS: ${text.length} chars" else "FAIL: file too short (${text.length} chars)"
        val passed = checks.all { it.second.startsWith("PASS") }
        return VerificationReport(passed, checks, checks.joinToString("; ") { it.second })
    }
}
