package com.raj.codex.context
import java.io.File
/** Never send the whole repo. Select only relevant files, prune old output. */
class ContextEngine(private val root: File) {
    private val ignored = setOf(".git", "node_modules", "build", ".gradle", ".idea")
    fun index(): List<File> = root.walkTopDown().filter { it.isFile && ignored.none { d -> it.path.contains("/$d/") } }.take(2000).toList()
    fun select(prompt: String): List<String> {
        val words = prompt.lowercase().split(Regex("\\W+")).filter { it.length > 3 }.toSet()
        val scored = index().map { f ->
            val name = f.name.lowercase()
            val score = words.count { name.contains(it) } * 3 + if (f.extension in setOf("html","kt","java","js","ts","md","json")) 1 else 0
            f to score
        }.filter { it.second > 0 }.sortedByDescending { it.second }.take(10)
        return scored.map { it.first.name + " (" + it.first.length() + " bytes)" }
    }
}
class ProjectMemory(private val root: File) {
    private val file = File(root, ".codex-memory.json")
    fun save(key: String, value: String) { val m = load().toMutableMap(); m[key] = value; file.parentFile?.mkdirs(); file.writeText(m.entries.joinToString("\n") { "${it.key}=${it.value}" }) }
    fun load(): Map<String, String> = if (!file.exists()) emptyMap() else file.readLines().mapNotNull { l -> l.split("=", limit = 2).takeIf { it.size == 2 }?.let { it[0] to it[1] } }.toMap()
}
