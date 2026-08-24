package com.ofh.harness

import org.json.JSONObject

/**
 * The OFH agent loop: plan → call LLM → execute tools → observe → loop.
 * Provider-agnostic via [ProviderAdapter]; tools via [ToolRegistry].
 */
class AgentLoop(
    private val adapter: ProviderAdapter,
    private val tools: ToolRegistry,
    private val persona: String = DEFAULT_PERSONA,
    private val maxIterations: Int = 8,
    private val budgetMs: Long = 120_000L,
    private val memory: Memory? = null,
    private val skills: Skills? = null
) {
    private val history = mutableListOf<ChatMessage>()

    /** Run one user turn; streams assistant text via [onDelta]. Returns the final answer. */
    fun runTurn(
        userMsg: String,
        onDelta: (String) -> Unit = {},
        onTool: (String, String, String) -> Unit = { _, _, _ -> }
    ): String {
        history.add(ChatMessage("user", userMsg))
        val deadline = System.currentTimeMillis() + budgetMs
        var answer = ""
        for (iter in 0 until maxIterations) {
            if (System.currentTimeMillis() > deadline) {
                answer = if (answer.isEmpty()) "(budget exceeded)" else answer
                break
            }
            val messages = listOf(ChatMessage("system", buildSystemPrompt())) + history
            val resp = adapter.completeStreaming(messages, tools.schemas(), onDelta)
            history.add(ChatMessage("assistant", resp.content, resp.toolCalls))
            if (resp.toolCalls.isEmpty()) {
                answer = resp.content
                break
            }
            // execute each tool call, append results, loop
            for (tc in resp.toolCalls) {
                val result = tools.dispatch(tc.name, tc.arguments)
                onTool(tc.name, tc.arguments, result)
                history.add(ChatMessage("tool", result, toolCallId = tc.id))
            }
        }
        return answer
    }

    private fun buildSystemPrompt(): String {
        val sb = StringBuilder(persona)
        skills?.let { s ->
            val names = s.list()
            if (names.isNotEmpty()) sb.append("\n\nAvailable skills: ").append(names.joinToString(", "))
        }
        memory?.let { m ->
            val notes = m.recent(3)
            if (notes.isNotEmpty()) sb.append("\n\nRelevant memory:\n").append(notes.joinToString("\n") { "- $it" })
        }
        return sb.toString()
    }

    fun reset() { history.clear() }

    companion object {
        const val DEFAULT_PERSONA =
            "You are OpenForest, a capable, direct, and slightly witty assistant. " +
            "You are grounded in real-time information when you can search. " +
            "You answer clearly, use tools when helpful, and never invent facts you can check."
    }
}
