package com.ofh.harness

import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL

/** A single chat message in the conversation. */
data class ChatMessage(
    val role: String,               // "system" | "user" | "assistant" | "tool"
    val content: String,
    val toolCalls: List<ToolCall> = emptyList(),
    val toolCallId: String? = null
)

/** A model-requested tool invocation. */
data class ToolCall(val id: String, val name: String, val arguments: String)

/** The provider's reply to one request. */
data class ProviderResponse(
    val content: String,
    val toolCalls: List<ToolCall>,
    val finishReason: String
)

/**
 * OpenAI-compatible chat-completions client (streaming + tool calls).
 * One adapter serves every provider (Ollama, MiniMax, Z.AI, Qwen, DeepSeek, …).
 */
class ProviderAdapter(
    private val baseUrl: String,
    private val model: String,
    private val apiKey: String,
    private val temperature: Double = 0.0,
    private val maxTokens: Int = 4096
) {

    /** POST /chat/completions (non-streaming). Returns the parsed response. */
    fun complete(messages: List<ChatMessage>, tools: List<JSONObject>): ProviderResponse {
        val body = JSONObject()
            .put("model", model)
            .put("messages", messagesToJson(messages))
            .put("temperature", temperature)
            .put("max_tokens", maxTokens)
            .put("stream", false)
        if (tools.isNotEmpty()) body.put("tools", JSONArray(tools))
        val resp = post(body)
        val choice = resp.getJSONArray("choices").getJSONObject(0)
        val msg = choice.getJSONObject("message")
        val content = msg.optString("content", "")
        val toolCalls = parseToolCalls(msg.optJSONArray("tool_calls"))
        val finish = choice.optString("finish_reason", "stop")
        return ProviderResponse(content, toolCalls, finish)
    }

    /** POST /chat/completions (streaming SSE). Calls [onDelta] per content chunk. */
    fun completeStreaming(
        messages: List<ChatMessage>,
        tools: List<JSONObject>,
        onDelta: (String) -> Unit
    ): ProviderResponse {
        val body = JSONObject()
            .put("model", model)
            .put("messages", messagesToJson(messages))
            .put("temperature", temperature)
            .put("max_tokens", maxTokens)
            .put("stream", true)
        if (tools.isNotEmpty()) body.put("tools", JSONArray(tools))

        val conn = open(body)
        val code = conn.responseCode
        if (code !in 200..299) throw RuntimeException("HTTP $code: ${conn.errorStream?.bufferedReader()?.readText() ?: ""}")

        val reader = BufferedReader(InputStreamReader(conn.inputStream))
        val content = StringBuilder()
        val toolCalls = mutableListOf<ToolCall>()
        var finish = "stop"
        var line: String?
        while (reader.readLine().also { line = it } != null) {
            val l = line!!.trim()
            if (!l.startsWith("data:")) continue
            val data = l.removePrefix("data:").trim()
            if (data == "[DONE]") break
            val json = runCatching { JSONObject(data) }.getOrNull() ?: continue
            val choice = json.optJSONArray("choices")?.optJSONObject(0) ?: continue
            val delta = choice.optJSONObject("delta")
            if (delta != null) {
                val piece = delta.optString("content", "")
                if (piece.isNotEmpty()) { content.append(piece); onDelta(piece) }
                val tc = delta.optJSONArray("tool_calls")
                if (tc != null) {
                    for (i in 0 until tc.length()) {
                        val t = tc.getJSONObject(i)
                        val id = t.optString("id", "call_$i")
                        val fn = t.optJSONObject("function")
                        val name = fn?.optString("name", "") ?: ""
                        val args = fn?.optString("arguments", "") ?: ""
                        val existing = toolCalls.firstOrNull { it.id == id }
                        if (existing != null) {
                            val merged = existing.copy(name = existing.name + name, arguments = existing.arguments + args)
                            toolCalls[toolCalls.indexOf(existing)] = merged
                        } else {
                            toolCalls.add(ToolCall(id, name, args))
                        }
                    }
                }
            }
            if (choice.has("finish_reason") && !choice.isNull("finish_reason")) finish = choice.getString("finish_reason")
        }
        reader.close()
        conn.disconnect()
        return ProviderResponse(content.toString(), toolCalls, finish)
    }

    private fun messagesToJson(messages: List<ChatMessage>): JSONArray {
        val arr = JSONArray()
        for (m in messages) {
            val o = JSONObject().put("role", m.role).put("content", m.content)
            if (m.toolCalls.isNotEmpty()) {
                val tcs = JSONArray()
                for (tc in m.toolCalls) {
                    tcs.put(JSONObject()
                        .put("id", tc.id)
                        .put("type", "function")
                        .put("function", JSONObject().put("name", tc.name).put("arguments", tc.arguments)))
                }
                o.put("tool_calls", tcs)
            }
            if (m.toolCallId != null) o.put("tool_call_id", m.toolCallId)
            arr.put(o)
        }
        return arr
    }

    private fun parseToolCalls(arr: JSONArray?): List<ToolCall> {
        if (arr == null) return emptyList()
        val out = mutableListOf<ToolCall>()
        for (i in 0 until arr.length()) {
            val t = arr.getJSONObject(i)
            val fn = t.optJSONObject("function")
            out.add(ToolCall(t.optString("id", "call_$i"), fn?.optString("name", "") ?: "", fn?.optString("arguments", "") ?: ""))
        }
        return out
    }

    private fun post(body: JSONObject): JSONObject {
        val conn = open(body)
        val code = conn.responseCode
        val text = if (code in 200..299) conn.inputStream.bufferedReader().readText() else conn.errorStream?.bufferedReader()?.readText() ?: ""
        conn.disconnect()
        if (code !in 200..299) throw RuntimeException("HTTP $code: $text")
        return JSONObject(text)
    }

    private fun open(body: JSONObject): HttpURLConnection {
        val url = URL(baseUrl.trimEnd('/') + "/chat/completions")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.connectTimeout = 20_000
        conn.readTimeout = 60_000
        conn.setRequestProperty("Content-Type", "application/json")
        conn.setRequestProperty("Accept", "application/json")
        if (apiKey.isNotEmpty()) conn.setRequestProperty("Authorization", "Bearer $apiKey")
        conn.doOutput = true
        val out: OutputStream = conn.outputStream
        out.write(body.toString().toByteArray())
        out.flush()
        out.close()
        return conn
    }
}
