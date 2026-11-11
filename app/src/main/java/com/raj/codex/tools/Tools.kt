package com.raj.codex.tools
import java.io.File
object FileTools {
    fun read(root: File, rel: String): String { val f = File(root, rel); require(com.raj.codex.security.PathGuard.isSafe(root.path, rel)) { "Unsafe path" }; return f.readText() }
    fun write(root: File, rel: String, content: String) { val f = File(root, rel); require(com.raj.codex.security.PathGuard.isSafe(root.path, rel)) { "Unsafe path" }; f.parentFile?.mkdirs(); f.writeText(content) }
    fun list(root: File): List<String> = root.walkTopDown().filter { it.isFile }.take(500).map { it.relativeTo(root).path }.toList()
    fun search(root: File, query: String): List<String> = root.walkTopDown().filter { it.isFile && it.length() < 200_000 }.filter { runCatching { it.readText().contains(query, ignoreCase = true) }.getOrDefault(false) }.take(50).map { it.relativeTo(root).path }.toList()
    /** Targeted replace, never whole-file rewrite unless needed. */
    fun patch(root: File, rel: String, old: String, new: String): Boolean { val t = read(root, rel); if (!t.contains(old)) return false; write(root, rel, t.replace(old, new)); return true }
}
/** Checkpoint = snapshot copy. Android has no git CLI, so recovery is snapshot-based. */
class CheckpointManager(private val root: File) {
    private val dir = File(root, ".codex-checkpoints")
    fun create(label: String): File { val dest = File(dir, label + "-" + System.currentTimeMillis()); dest.mkdirs(); root.walkTopDown().filter { it.isFile && !it.path.contains(".codex-checkpoints") }.take(500).forEach { f -> val d = File(dest, f.relativeTo(root).path); d.parentFile?.mkdirs(); runCatching { f.copyTo(d, overwrite = true) } }; return dest }
    fun list(): List<String> = if (!dir.exists()) emptyList() else dir.listFiles()?.map { it.name }?.sorted() ?: emptyList()
    fun restore(label: String): Boolean { val src = File(dir, label); if (!src.exists()) return false; src.walkTopDown().filter { it.isFile }.forEach { f -> val d = File(root, f.relativeTo(src).path); d.parentFile?.mkdirs(); f.copyTo(d, overwrite = true) }; return true }
}
