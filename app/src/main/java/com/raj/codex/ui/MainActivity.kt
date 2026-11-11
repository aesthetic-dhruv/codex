package com.raj.codex.ui
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.raj.codex.agents.CodexTask
import com.raj.codex.agents.TaskEngine
import com.raj.codex.data.Project
import com.raj.codex.data.ProjectStore
import com.raj.codex.databinding.ActivityMainBinding
import com.raj.codex.gateway.ModelGateway
import com.raj.codex.router.RoutingMode
import com.raj.codex.security.SecretStore
import kotlinx.coroutines.launch
import java.util.UUID
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var store: ProjectStore
    private lateinit var secrets: SecretStore
    private var currentProjectId: String? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        store = ProjectStore(this); secrets = SecretStore(this)
        binding.appTypeSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, listOf("Web App (preview phone me)", "Android App (code + cloud APK)"))
        binding.modeSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, RoutingMode.values().map { it.name })
        binding.generateBtn.setOnClickListener { generate() }
        binding.previewBtn.setOnClickListener {
            val id = currentProjectId ?: run { Toast.makeText(this, "Pehle App Banao dabao", Toast.LENGTH_SHORT).show(); return@setOnClickListener }
            startActivity(Intent(this, PreviewActivity::class.java).putExtra("projectId", id))
        }
        binding.buildApkBtn.setOnClickListener {
            binding.statusText.text = "Build APK: APK phone ke andar nahi banta. Code GitHub (aesthetic-dhruv/codex) pe push hoga, wahan Actions se APK banega, phir download karke Transfer kar sakte ho. Ye v1 me manual step hai — repo bante hi auto hoga."
            Toast.makeText(this, "Cloud build ke liye GitHub repo chahiye: aesthetic-dhruv/codex", Toast.LENGTH_LONG).show()
        }
        binding.transferBtn.setOnClickListener { startActivity(Intent(this, TransferActivity::class.java).putExtra("projectId", currentProjectId)) }
        binding.providersBtn.setOnClickListener { startActivity(Intent(this, ProvidersActivity::class.java)) }
    }
    private fun generate() {
        val prompt = binding.promptInput.text.toString().trim()
        if (prompt.isEmpty()) { Toast.makeText(this, "Pehle prompt likho", Toast.LENGTH_SHORT).show(); return }
        if (!secrets.hasKey("gemini") && !secrets.hasKey("openrouter") && !secrets.hasKey("groq")) {
            binding.statusText.text = "Status: koi API key nahi hai. Providers aur Health me jaake Gemini ya OpenRouter ki key daalo, bina key ke AI kaam nahi karega."
            startActivity(Intent(this, ProvidersActivity::class.java)); return
        }
        val id = "proj-" + UUID.randomUUID().toString().take(8)
        currentProjectId = id
        val dir = store.dir(id)
        val mode = RoutingMode.valueOf(binding.modeSpinner.selectedItem.toString())
        store.save(Project(id, prompt.take(40), prompt, System.currentTimeMillis(), "RUNNING"))
        binding.statusText.text = "Status: kaam chal raha hai — Manager > Planner > Coder > Tester > Reviewer"
        lifecycleScope.launch {
            val task = CodexTask(id, prompt, dir)
            TaskEngine(ModelGateway(secrets), mode).run(task) { state, msg ->
                runOnUiThread { binding.statusText.text = "Status: $state — $msg"; binding.logText.text = task.log.toString() }
            }
            store.save(Project(id, prompt.take(40), prompt, System.currentTimeMillis(), task.state.name))
            runOnUiThread { binding.logText.text = task.log.toString(); Toast.makeText(this@MainActivity, "Result: ${task.state}", Toast.LENGTH_LONG).show() }
        }
    }
}
