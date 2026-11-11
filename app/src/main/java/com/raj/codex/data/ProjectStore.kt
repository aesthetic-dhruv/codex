package com.raj.codex.data
import android.content.Context
import com.google.gson.Gson
import java.io.File
data class Project(val id: String, val name: String, val prompt: String, val createdAtMs: Long, val lastStatus: String)
/** Projects live in app files dir: Codex/<project-id>/index.html */
class ProjectStore(context: Context) {
    private val base = File(context.filesDir, "Codex").apply { mkdirs() }
    private val gson = Gson()
    fun dir(id: String): File = File(base, id).apply { mkdirs() }
    fun list(): List<Project> = base.listFiles()?.filter { it.isDirectory }?.mapNotNull { d -> runCatching { gson.fromJson(File(d, "project.json").readText(), Project::class.java) }.getOrNull() }?.sortedByDescending { it.createdAtMs } ?: emptyList()
    fun save(p: Project) { val d = dir(p.id); File(d, "project.json").writeText(gson.toJson(p)) }
}
