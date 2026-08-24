package com.ofh.harness

import android.content.Context
import java.io.File

/** Loads skills from ~/.ofh/skills/ (each a directory with a SKILL.md). */
class Skills(private val dir: File) {

    fun list(): List<String> {
        if (!dir.exists()) return emptyList()
        return dir.listFiles()?.filter { it.isDirectory }?.map { it.name }?.sorted() ?: emptyList()
    }

    fun read(name: String): String {
        val f = File(File(dir, name), "SKILL.md")
        return if (f.exists()) f.readText() else ""
    }

    fun allContent(): String =
        list().joinToString("\n\n") { name -> "## $name\n${read(name)}" }

    companion object {
        fun forContext(context: Context): Skills = Skills(File(File(context.filesDir, ".ofh"), "skills"))
    }
}
