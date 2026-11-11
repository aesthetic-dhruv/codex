package com.raj.codex.ui
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.raj.codex.gateway.ModelGateway
import com.raj.codex.security.SecretStore
import kotlinx.coroutines.launch
class ProvidersActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(32, 32, 32, 32) }
        val info = TextView(this).apply { text = "Providers — Health tabhi dikhega jab asli check chalega. Key sirf is phone me save hoti hai, kahin nahi bheji jati." }
        val keyInput = EditText(this).apply { hint = "Gemini API key yahan daalo" }
        val saveBtn = Button(this).apply { text = "Gemini Key Save Karo" }
        val checkBtn = Button(this).apply { text = "Health Check Chalao" }
        val result = TextView(this).apply { text = "Abhi check nahi hua." }
        layout.addView(info); layout.addView(keyInput); layout.addView(saveBtn); layout.addView(checkBtn); layout.addView(result)
        setContentView(layout)
        val secrets = SecretStore(this); val gateway = ModelGateway(secrets)
        saveBtn.setOnClickListener { val k = keyInput.text.toString().trim(); if (k.isNotEmpty()) { secrets.saveKey("gemini", k); Toast.makeText(this, "Key save ho gayi", Toast.LENGTH_SHORT).show() } }
        checkBtn.setOnClickListener {
            lifecycleScope.launch {
                val sb = StringBuilder()
                for (p in listOf("gemini", "openrouter", "groq", "nvidia", "ollama")) {
                    val h = gateway.health(p); sb.appendLine("$p: ${h.status} — ${h.detail}")
                }
                runOnUiThread { result.text = sb.toString() }
            }
        }
    }
}
