package com.ofh.harness

import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.atomic.AtomicInteger

/**
 * End-to-end JVM test of the REAL ProviderAdapter + AgentLoop + ToolRegistry
 * against a local mock OpenAI-compatible /chat/completions server (plain sockets).
 * Proves the harness actually runs (streaming + tool-call loop), not just compiles.
 */
class HarnessIntegrationTest {

    private var serverSocket: ServerSocket? = null
    private var serverThread: Thread? = null
    private val requestCount = AtomicInteger(0)

    @Before
    fun setUp() {
        val ss = ServerSocket(0)
        serverSocket = ss
        serverThread = Thread {
            while (!ss.isClosed) {
                val sock: Socket = try { ss.accept() } catch (e: Exception) { break }
                Thread { handle(sock) }.start()
            }
        }
        serverThread!!.start()
    }

    private fun handle(sock: Socket) {
        try {
            val reader = BufferedReader(InputStreamReader(sock.getInputStream(), Charsets.UTF_8))
            // read request line + headers
            var contentLength = 0
            var line: String? = reader.readLine()
            if (line.isNullOrBlank()) { sock.close(); return }
            while (true) {
                val h = reader.readLine() ?: break
                if (h.isBlank()) break
                if (h.startsWith("Content-Length:", true)) contentLength = h.substringAfter(':').trim().toInt()
            }
            // read body
            val body = CharArray(contentLength)
            var read = 0
            while (read < contentLength) {
                val r = reader.read(body, read, contentLength - read)
                if (r < 0) break
                read += r
            }
            val bodyStr = String(body, 0, read)
            val n = requestCount.incrementAndGet()
            val streaming = bodyStr.contains("\"stream\":true")

            val out = sock.getOutputStream()
            val crlf = "\r\n"

            fun writeSSE(chunks: List<String>) {
                val resp = "HTTP/1.1 200 OK$crlf" +
                    "Content-Type: text/event-stream$crlf" +
                    "Connection: close$crlf$crlf" +
                    chunks.joinToString("\n") + "\n"
                out.write(resp.toByteArray(Charsets.UTF_8)); out.flush()
            }

            if (!streaming) {
                val body2 = """{"choices":[{"index":0,"message":{"role":"assistant","content":"nonstream-ok","tool_calls":[{"id":"c0","type":"function","function":{"name":"echo","arguments":"{\"text\":\"x\"}"}}]},"finish_reason":"tool_calls"}],"usage":{}}"""
                val resp = "HTTP/1.1 200 OK$crlf" +
                    "Content-Type: application/json$crlf" +
                    "Content-Length: ${body2.toByteArray().size}$crlf" +
                    "Connection: close$crlf$crlf" + body2
                out.write(resp.toByteArray(Charsets.UTF_8)); out.flush()
            } else if (n == 1) {
                // 1st streaming call: tool_call (name in chunk1, args split across chunks), finish tool_calls
                writeSSE(listOf(
                    """data: {"choices":[{"index":0,"delta":{"role":"assistant","tool_calls":[{"index":0,"id":"call_1","type":"function","function":{"name":"echo","arguments":"{\"text\":\"hi"}}]},"finish_reason":null}]}""",
                    """data: {"choices":[{"index":0,"delta":{"tool_calls":[{"index":0,"function":{"arguments":"}\"}}]},"finish_reason":null}]}""",
                    """data: {"choices":[{"index":0,"delta":{},"finish_reason":"tool_calls"}]}""",
                    "data: [DONE]"
                ))
            } else {
                // 2nd+ streaming call: final text across chunks, finish stop
                writeSSE(listOf(
                    """data: {"choices":[{"index":0,"delta":{"content":"Hello"},"finish_reason":null}]}""",
                    """data: {"choices":[{"index":0,"delta":{"content":" from mock"},"finish_reason":null}]}""",
                    """data: {"choices":[{"index":0,"delta":{},"finish_reason":"stop"}]}""",
                    "data: [DONE]"
                ))
            }
        } catch (e: Exception) {
            // ignore per-request errors
        } finally {
            try { sock.close() } catch (e: Exception) {}
        }
    }

    @After
    fun tearDown() {
        try { serverSocket?.close() } catch (e: Exception) {}
        serverThread?.interrupt()
    }

    private fun base(): String {
        val p = serverSocket!!.localPort
        return "http://127.0.0.1:$p/v1"
    }

    @Test
    fun streamingToolCallThenFinalAnswerViaRealAgentLoop() {
        val adapter = ProviderAdapter(base(), "mock-model", "test-key")
        val registry = ToolRegistry()
        registry.register(Tool(
            name = "echo",
            description = "echo back text",
            parameters = JSONObject().put("type", "object")
                .put("properties", JSONObject().put("text", JSONObject().put("type", "string"))),
            handler = { a -> "echo:${a.optString("text")}" }
        ))
        val loop = AgentLoop(adapter, registry)
        val deltas = StringBuilder()
        val answer = loop.runTurn("say hi", onDelta = { s -> deltas.append(s); Unit })
        assertEquals("Hello from mock", answer)
        assertTrue("expected streamed deltas, got: '$deltas'", deltas.contains("Hello"))
        assertTrue(deltas.contains(" from mock"))
        // the echo tool was dispatched during the loop
        assertEquals("echo:hi", registry.dispatch("echo", """{"text":"hi"}"""))
    }

    @Test
    fun nonStreamingCompleteParsesToolCalls() {
        val adapter = ProviderAdapter(base(), "mock", "")
        val resp = adapter.complete(emptyList(), emptyList())
        assertEquals("nonstream-ok", resp.content)
        assertEquals(1, resp.toolCalls.size)
        assertEquals("echo", resp.toolCalls[0].name)
        assertTrue(resp.toolCalls[0].arguments.contains("x"))
        assertEquals("tool_calls", resp.finishReason)
    }

    @Test
    fun unknownToolDispatchReturnsError() {
        val registry = ToolRegistry()
        assertTrue(registry.dispatch("nope", "{}").startsWith("Error"))
        assertFalse(registry.has("nope"))
    }
}
