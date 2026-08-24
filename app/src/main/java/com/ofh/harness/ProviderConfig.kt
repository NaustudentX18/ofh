package com.ofh.harness

import android.content.Context
import java.io.File

/**
 * OFH provider/model configuration.
 *
 * Stored under `~/.ofh/`:
 *  - `.credentials.yaml`  → `version: 1` + `refs: { KEY_ENV: "…" }`
 *  - `settings.yaml`      → `llm-pi-ai.providers.<route>` + `agent-default-model`
 *
 * Provider-agnostic: one OpenAI-compatible profile per provider.
 */
object ProviderConfig {

    data class Preset(
        val route: String,
        val label: String,
        val baseURL: String,
        val defaultModel: String,
        val keyEnv: String,
        val needsKey: Boolean,
        val keyHint: String
    )

    val providers = listOf(
        Preset("ollamacloud", "Ollama Cloud", "https://ollama.com/v1", "deepseek-v4-flash:0731", "OLLAMACLOUD_API_KEY", true, "Ollama Cloud API key"),
        Preset("ollama", "Ollama Local", "http://127.0.0.1:11434/v1", "qwen3:8b", "OLLAMA_API_KEY", false, "no key needed"),
        Preset("minimax", "MiniMax Coding", "https://api.minimaxi.com/v1", "MiniMax-M2", "MINIMAX_API_KEY", true, "MiniMax API key"),
        Preset("zai", "Z.AI Coding", "https://api.z.ai/api/paas/v4", "glm-4.7", "ZAI_API_KEY", true, "Z.AI API key"),
        Preset("qwen", "Qwen Coding", "https://dashscope-intl.aliyuncs.com/compatible-mode/v1", "qwen3-coder-plus", "DASHSCOPE_API_KEY", true, "DashScope API key"),
        Preset("deepseek", "DeepSeek", "https://api.deepseek.com/v1", "deepseek-chat", "DEEPSEEK_API_KEY", true, "DeepSeek API key"),
        Preset("openai", "OpenAI", "https://api.openai.com/v1", "gpt-4.1", "OPENAI_API_KEY", true, "OpenAI API key"),
        Preset("custom", "Custom", "", "", "CUSTOM_API_KEY", true, "API key"),
    )

    fun preset(route: String): Preset = providers.first { it.route == route }

    fun ofhDir(context: Context): File = File(context.filesDir, ".ofh")

    /** Persist provider + model + key. */
    fun save(context: Context, p: Preset, baseUrl: String, model: String, key: String): Boolean =
        runCatching {
            val dir = ofhDir(context); dir.mkdirs()
            if (p.needsKey && key.isNotEmpty()) writeCredentialRef(p.keyEnv, key, dir)
            writeSettings(p, baseUrl, model, dir)
        }.isSuccess

    fun readSavedKey(context: Context, route: String): String {
        val f = File(ofhDir(context), ".credentials.yaml")
        if (!f.exists()) return ""
        val env = preset(route).keyEnv
        return runCatching {
            f.readLines().firstOrNull { it.trimStart().startsWith("$env:") }
                ?.substringAfter(":")?.trim()?.trim('"').orEmpty()
        }.getOrDefault("")
    }

    fun readCurrent(context: Context): Pair<String, String>? {
        val f = File(ofhDir(context), "settings.yaml")
        if (!f.exists()) return null
        return runCatching {
            var provider: String? = null; var model: String? = null; var inDefault = false
            for (line in f.readLines()) {
                if (line.startsWith("agent-default-model:")) { inDefault = true; continue }
                if (inDefault && line.isNotEmpty() && !line.startsWith(" ") && !line.startsWith("\t")) break
                if (inDefault) {
                    val t = line.trim()
                    if (t.startsWith("provider:")) provider = t.substringAfter(":").trim().trim('"')
                    if (t.startsWith("model:")) model = t.substringAfter(":").trim().trim('"')
                }
            }
            if (provider != null && model != null) provider to model else null
        }.getOrNull()
    }

    /** Read the persisted baseURL for [route] from settings.yaml; falls back to the preset. */
    fun readBaseUrl(context: Context, route: String): String {
        val f = File(ofhDir(context), "settings.yaml")
        if (!f.exists()) return preset(route).baseURL
        return runCatching {
            val lines = f.readLines()
            var i = 0
            while (i < lines.size) {
                if (lines[i].trim() == "$route:") {
                    var j = i + 1
                    while (j < lines.size) {
                        val line = lines[j]
                        if (line.isNotEmpty() && !line.startsWith(" ") && !line.startsWith("\t")) break
                        val t = line.trim()
                        if (t.startsWith("baseURL:")) return@runCatching t.substringAfter(":").trim().trim('"')
                        j++
                    }
                }
                i++
            }
            preset(route).baseURL
        }.getOrDefault(preset(route).baseURL)
    }

    /** Persist the active provider + model (keeps the existing baseURL/key). */
    fun setCurrent(context: Context, route: String, model: String): Boolean =
        runCatching {
            val p = preset(route)
            val dir = ofhDir(context); dir.mkdirs()
            val baseUrl = readBaseUrl(context, route).ifEmpty { p.baseURL }
            writeSettings(p, baseUrl, model, dir)
        }.isSuccess

    private fun writeCredentialRef(env: String, key: String, dir: File) {
        val f = File(dir, ".credentials.yaml")
        val text = if (f.exists()) f.readText() else "version: 1\nrefs:\n"
        val lines = text.split("\n").toMutableList()
        val refLine = "  $env: \"${key.replace("\"", "\\\"")}\""
        val existingIdx = lines.indexOfFirst { it.trimStart().startsWith("$env:") }
        if (existingIdx >= 0) lines[existingIdx] = refLine
        else {
            val refIdx = lines.indexOfFirst { it.trimStart().startsWith("refs:") }
            if (refIdx >= 0) lines.add(refIdx + 1, refLine)
            else { if (lines.none { it.trimStart().startsWith("version:") }) lines.add(0, "version: 1"); lines.add("refs:"); lines.add(refLine) }
        }
        f.writeText(lines.joinToString("\n"))
    }

    private fun writeSettings(p: Preset, baseUrl: String, model: String, dir: File) {
        val f = File(dir, "settings.yaml")
        val providerBlock = buildString {
            appendLine("llm-pi-ai:")
            appendLine("  providers:")
            appendLine("    ${p.route}:")
            appendLine("      displayName: \"${p.label}\"")
            appendLine("      api: openai-completions")
            if (p.needsKey) appendLine("      apiKeyEnv: ${p.keyEnv}")
            appendLine("      baseURL: \"$baseUrl\"")
            appendLine("      models:")
            appendLine("        - id: \"$model\"")
        }
        val modelBlock = buildString {
            appendLine("agent-default-model:")
            appendLine("  provider: ${p.route}")
            appendLine("  model: \"$model\"")
        }
        val existing = if (f.exists()) f.readText() else ""
        val step1 = upsertTopKey(existing, "llm-pi-ai", providerBlock)
        f.writeText(upsertTopKey(step1, "agent-default-model", modelBlock))
    }

    private fun upsertTopKey(text: String, key: String, block: String): String {
        val lines = text.split("\n")
        val idx = lines.indexOfFirst { it == "$key:" }
        if (idx == -1) { val base = text.trimEnd('\n'); return (if (base.isEmpty()) "" else base + "\n") + block.trimEnd('\n') + "\n" }
        val after = lines.drop(idx + 1)
        val end = after.indexOfFirst { it.isNotEmpty() && !it.startsWith(" ") && !it.startsWith("\t") }
        val endIdx = if (end == -1) lines.size else idx + 1 + end
        val before = lines.subList(0, idx)
        val tail = if (end == -1) emptyList() else lines.subList(endIdx, lines.size)
        return (before + block.trimEnd('\n').split("\n") + tail).joinToString("\n")
    }
}
