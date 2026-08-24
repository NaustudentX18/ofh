package com.ofh.harness

import android.content.Context
import java.io.File

/** Simple long-term memory: timestamped notes under ~/.ofh/memory/. */
class Memory(private val dir: File) {

    fun add(note: String) {
        dir.mkdirs()
        File(dir, "notes.txt").appendText("${System.currentTimeMillis()}\t$note\n")
    }

    fun recent(n: Int): List<String> {
        val f = File(dir, "notes.txt")
        if (!f.exists()) return emptyList()
        return f.readLines().takeLast(n).map { it.substringAfter('\t') }
    }

    fun all(): List<String> {
        val f = File(dir, "notes.txt")
        if (!f.exists()) return emptyList()
        return f.readLines().map { it.substringAfter('\t') }
    }

    companion object {
        fun forContext(context: Context): Memory = Memory(File(File(context.filesDir, ".ofh"), "memory"))
    }
}
