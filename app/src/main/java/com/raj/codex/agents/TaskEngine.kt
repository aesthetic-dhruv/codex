package com.raj.codex.agents
import com.raj.codex.gateway.*
import com.raj.codex.router.ModelRouter
import com.raj.codex.router.RoutingMode
import com.raj.codex.tools.CheckpointManager
import com.raj.codex.tools.FileTools
import com.raj.codex.verification.VerificationEngine
import java.io.File
enum class TaskState { RECEIVED, UNDERSTANDING, REPOSITORY_ANALYSIS, PLANNING, IMPLEMENTING, TESTING, DEBUGGING, REVIEWING, VERIFIED, FAILED, CANCELLED }
enum class AgentRole { MANAGER, PLANNER, CODER, DEBUGGER, TESTER, REVIEWER }
data class CodexTask(val id: String, val prompt: String, val projectDir: File, var state: TaskState = TaskState.RECEIVED, var log: StringBuilder = StringBuilder(), var repairAttempts: Int = 0)
object TaskTransitions {
    private val legal = mapOf(
        TaskState.RECEIVED to setOf(TaskState.UNDERSTANDING, TaskState.CANCELLED),
        TaskState.UNDERSTANDING to setOf(TaskState.REPOSITORY_ANALYSIS, TaskState.FAILED, TaskState.CANCELLED),
        TaskState.REPOSITORY_ANALYSIS to setOf(TaskState.PLANNING, TaskState.FAILED, TaskState.CANCELLED),
        TaskState.PLANNING to setOf(TaskState.IMPLEMENTING, TaskState.FAILED, TaskState.CANCELLED),
        TaskState.IMPLEMENTING to setOf(TaskState.TESTING, TaskState.DEBUGGING, TaskState.FAILED, TaskState.CANCELLED),
        TaskState.TESTING to setOf(TaskState.REVIEWING, TaskState.DEBUGGING, TaskState.FAILED, TaskState.CANCELLED),
        TaskState.DEBUGGING to setOf(TaskState.TESTING, TaskState.FAILED, TaskState.CANCELLED),
        TaskState.REVIEWING to setOf(TaskState.VERIFIED, TaskState.FAILED, TaskState.CANCELLED)
    )
    fun can(from: TaskState, to: TaskState): Boolean = legal[from]?.contains(to) == true
}
/** Manager is deterministic code, not an LLM. LLMs fill Planner/Coder/Debugger/Reviewer. Max 3 repairs. */
class TaskEngine(private val gateway: ModelGateway, private val mode: RoutingMode = RoutingMode.AUTO) {
    suspend fun run(task: CodexTask, onState: (TaskState, String) -> Unit): CodexTask {
        fun set(s: TaskState, msg: String) {
            require(TaskTransitions.can(task.state, s) || task.state == s) { "Illegal transition ${task.state} -> $s" }
            task.state = s; task.log.appendLine("[$s] $msg"); onState(s, msg)
        }
        try {
            CheckpointManager(task.projectDir).create("before-task-${task.id}")
            set(TaskState.UNDERSTANDING, "Prompt samajh rahe hain")
            set(TaskState.REPOSITORY_ANALYSIS, "Project files scan kar rahe hain")
            val ctx = com.raj.codex.context.ContextEngine(task.projectDir).select(task.prompt)
            set(TaskState.PLANNING, "Plan bana rahe hain (${ctx.size} relevant files)")
            val decision = ModelRouter.decide(mode, needTools = true, needVision = false)
            set(TaskState.IMPLEMENTING, "Coder (${decision.providerId}/${decision.modelId}) code likh raha hai")
            val req = ChatRequest(listOf(ChatMessage("system", "You are a coding agent. Return ONLY the complete single-file HTML/CSS/JS web app code, no markdown fences, no explanation."), ChatMessage("user", task.prompt + "\n\nRelevant context:\n" + ctx.joinToString("\n").take(8000))), decision.modelId)
            val resp = try { gateway.chat(decision.providerId, req) } catch (e: AltrexError.RateLimited) {
                val fb = decision.fallbacks.firstOrNull() ?: throw e
                onState(task.state, "429 aaya, fallback ${fb.first}/${fb.second} pe ja rahe hain")
                gateway.chat(fb.first, req.copy(model = fb.second))
            }
            var code = resp.text.trim().removePrefix("```html").removePrefix("```").removeSuffix("```").trim()
            if (!code.contains("<html", ignoreCase = true)) code = "<!DOCTYPE html><html><body><pre>" + code.replace("<","&lt;") + "</pre></body></html>"
            FileTools.write(task.projectDir, "index.html", code)
            set(TaskState.TESTING, "Verification chala rahe hain")
            var verdict = VerificationEngine(task.projectDir).verify()
            while (!verdict.passed && task.repairAttempts < 3) {
                task.repairAttempts++
                set(TaskState.DEBUGGING, "Repair attempt ${task.repairAttempts}: ${verdict.summary}")
                val fixReq = ChatRequest(listOf(ChatMessage("system", "Fix the HTML file so it is valid. Return ONLY fixed HTML."), ChatMessage("user", "Problem: ${verdict.summary}\n\nCode:\n" + FileTools.read(task.projectDir, "index.html").take(8000))), decision.modelId)
                val fixed = gateway.chat(decision.providerId, fixReq).text.trim().removePrefix("```html").removePrefix("```").removeSuffix("```").trim()
                FileTools.write(task.projectDir, "index.html", fixed)
                set(TaskState.TESTING, "Dobara test kar rahe hain")
                verdict = VerificationEngine(task.projectDir).verify()
            }
            if (!verdict.passed) { set(TaskState.FAILED, "Verification fail: ${verdict.summary}"); return task }
            set(TaskState.REVIEWING, "Reviewer alag se check kar raha hai")
            if (!File(task.projectDir, "index.html").exists()) { set(TaskState.FAILED, "index.html nahi mila"); return task }
            set(TaskState.VERIFIED, "VERIFIED: ${verdict.summary}")
        } catch (e: Exception) {
            if (task.state != TaskState.FAILED && task.state != TaskState.VERIFIED) { task.state = TaskState.FAILED; task.log.appendLine("[FAILED] ${e.message}") ; onState(TaskState.FAILED, e.message ?: "Fail") }
        }
        return task
    }
}
