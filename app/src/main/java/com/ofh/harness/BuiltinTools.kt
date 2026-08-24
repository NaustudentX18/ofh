package com.ofh.harness

import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.StatFs
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** Registers OFH's built-in tools (real-time grounding, device, memory). */
object BuiltinTools {

    fun registerAll(registry: ToolRegistry, context: Context? = null, memory: Memory? = null, skills: Skills? = null) {
        registry.register(webSearch())
        registry.register(webFetch())
        if (context != null) registry.register(deviceInfo(context))
        if (memory != null) registry.register(memoryTool(memory))
        if (skills != null) registry.register(skillsTool(skills))
    }

    /** Grok-style live web search over free, no-key backends. */
    private fun webSearch(): Tool = Tool(
        name = "web_search",
        description = "Search the web for current information. Backends: wikipedia, stackoverflow, hackernews, npm, mdn, archive.",
        parameters = JSONObject()
            .put("type", "object")
            .put("properties", JSONObject()
                .put("query", JSONObject().put("type", "string").put("description", "search query"))
                .put("backend", JSONObject().put("type", "string").put("enum", JSONArray(listOf("wikipedia", "stackoverflow", "hackernews", "npm", "mdn", "archive")))))
            .put("required", JSONArray(listOf("query"))),
        handler = { args ->
            val q = args.optString("query", "")
            val backend = args.optString("backend", "wikipedia")
            when (backend) {
                "wikipedia" -> get("https://en.wikipedia.org/w/api.php?action=query&list=search&srsearch=${enc(q)}&format=json&srlimit=5")
                "stackoverflow" -> get("https://api.stackexchange.com/2.3/search/advanced?order=desc&sort=relevance&q=${enc(q)}&site=stackoverflow&pagesize=5")
                "hackernews" -> get("https://hn.algolia.com/api/v1/search?query=${enc(q)}&hitsPerPage=5")
                "npm" -> get("https://registry.npmjs.org/-/v1/search?text=${enc(q)}&size=5")
                "mdn" -> get("https://developer.mozilla.org/api/v1/search?q=${enc(q)}&locale=en-US")
                "archive" -> get("https://web.archive.org/cdx/search/cdx?url=${enc(q)}&output=json&limit=5&fl=timestamp,original")
                else -> "Unknown backend: $backend"
            }
        }
    )

    private fun webFetch(): Tool = Tool(
        name = "web_fetch",
        description = "Fetch the text content of a URL (HTTPS).",
        parameters = JSONObject()
            .put("type", "object")
            .put("properties", JSONObject().put("url", JSONObject().put("type", "string")))
            .put("required", JSONArray(listOf("url"))),
        handler = { args ->
            val url = args.optString("url", "")
            if (!url.startsWith("https://")) return@Tool "Error: only https:// URLs are allowed"
            runCatching {
                val conn = URL(url).openConnection() as HttpURLConnection
                conn.connectTimeout = 15_000; conn.readTimeout = 15_000
                conn.setRequestProperty("User-Agent", "OFH/0.1")
                val text = conn.inputStream.bufferedReader().readText()
                conn.disconnect()
                text.take(4000)
            }.getOrElse { "Error: ${it.message}" }
        }
    )

    private fun deviceInfo(context: Context): Tool = Tool(
        name = "device_info",
        description = "Return basic device info (model, Android version, free storage).",
        parameters = JSONObject().put("type", "object").put("properties", JSONObject()),
        handler = {
            val stat = StatFs(Environment.getDataDirectory().path)
            val freeMb = stat.availableBytes / (1024 * 1024)
            "model=${Build.MODEL}, android=${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT}), freeStorage=${freeMb}MB"
        }
    )

    private fun memoryTool(memory: Memory): Tool = Tool(
        name = "memory",
        description = "Store or recall long-term memory. action: 'set' (store a note) or 'get' (recall notes).",
        parameters = JSONObject()
            .put("type", "object")
            .put("properties", JSONObject()
                .put("action", JSONObject().put("type", "string").put("enum", JSONArray(listOf("set", "get"))))
                .put("note", JSONObject().put("type", "string").put("description", "the note to store (for action=set)")))
            .put("required", JSONArray(listOf("action"))),
        handler = { args ->
            when (args.optString("action", "get")) {
                "set" -> {
                    val n = args.optString("note", "")
                    if (n.isBlank()) "Error: note is empty" else { memory.add(n); "stored" }
                }
                else -> memory.all().joinToString("\n").ifEmpty { "(no memory yet)" }
            }
        }
    )

    private fun skillsTool(skills: Skills): Tool = Tool(
        name = "skills",
        description = "List available skills or read a skill's SKILL.md content.",
        parameters = JSONObject()
            .put("type", "object")
            .put("properties", JSONObject()
                .put("name", JSONObject().put("type", "string").put("description", "skill name to read (omit to list)")))
            .put("required", JSONArray()),
        handler = { args ->
            val name = args.optString("name", "")
            if (name.isEmpty()) skills.list().joinToString(", ").ifEmpty { "(no skills)" }
            else skills.read(name).ifEmpty { "No skill named '$name'" }
        }
    )

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")

    private fun get(url: String): String = runCatching {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 15_000; conn.readTimeout = 15_000
        conn.setRequestProperty("User-Agent", "OFH/0.1")
        val text = conn.inputStream.bufferedReader().readText()
        conn.disconnect()
        text.take(4000)
    }.getOrElse { "Error: ${it.message}" }
}
