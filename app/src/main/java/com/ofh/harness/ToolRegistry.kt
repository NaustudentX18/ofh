package com.ofh.harness

import org.json.JSONObject

/** A callable tool the model can invoke. */
data class Tool(
    val name: String,
    val description: String,
    val parameters: JSONObject,
    val handler: (JSONObject) -> String
) {
    /** OpenAI tool schema for the chat-completions `tools` array. */
    fun schema(): JSONObject = JSONObject()
        .put("type", "function")
        .put("function", JSONObject()
            .put("name", name)
            .put("description", description)
            .put("parameters", parameters))
}

/** Registry + dispatcher for the agent's tools. */
class ToolRegistry {
    private val tools = LinkedHashMap<String, Tool>()

    fun register(tool: Tool) { tools[tool.name] = tool }

    fun all(): List<Tool> = tools.values.toList()

    fun schemas(): List<JSONObject> = tools.values.map { it.schema() }

    fun has(name: String): Boolean = tools.containsKey(name)

    /** Execute a tool by name; returns the result string (or an error). */
    fun dispatch(name: String, argsJson: String): String {
        val tool = tools[name] ?: return "Error: unknown tool '$name'"
        return runCatching {
            val args = if (argsJson.isBlank()) JSONObject() else JSONObject(argsJson)
            tool.handler(args)
        }.getOrElse { "Error: ${it.message ?: "tool failed"}" }
    }
}
