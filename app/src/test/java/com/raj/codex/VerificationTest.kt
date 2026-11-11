package com.raj.codex
import com.raj.codex.verification.VerificationEngine
import org.junit.Assert.*
import org.junit.Test
import java.io.File
class VerificationTest {
    @Test fun missingFileFails() { val dir = File.createTempFile("codex-test", "").apply { delete(); mkdir() }; assertFalse(VerificationEngine(dir).verify().passed); dir.deleteRecursively() }
    @Test fun validHtmlPasses() { val dir = File.createTempFile("codex-test", "").apply { delete(); mkdir() }; File(dir, "index.html").writeText("<html><body>" + "x".repeat(300) + "</body></html>"); assertTrue(VerificationEngine(dir).verify().passed); dir.deleteRecursively() }
}
