package com.ofh.harness

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject

/**
 * Hosts the OFH web chat UI and bridges it to the agent loop.
 * Provider-agnostic: reads the active provider from [ProviderConfig].
 */
class WebViewActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private var busy = false

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        webView = WebView(this)
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.webViewClient = WebViewClient()
        webView.addJavascriptInterface(Bridge(), "OFH")
        setContentView(webView)
        webView.loadUrl("file:///android_asset/web/index.html")
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) webView.goBack() else super.onBackPressed()
    }

    /** JS bridge exposed as `window.OFH`. */
    inner class Bridge {
        @JavascriptInterface
        fun send(text: String, callback: String) {
            if (busy) return
            busy = true
            Thread {
                try {
                    val loop = buildLoop()
                    val answer = loop.runTurn(
                        text,
                        { delta -> runOnUiThread { webView.evaluateJavascript("window.OFH.onDelta && window.OFH.onDelta(${json(delta)});", null) } },
                        { name, args, result -> runOnUiThread { webView.evaluateJavascript("window.OFH.onTool && window.OFH.onTool(${toolJson(name, args, result)});", null) } }
                    )
                    runOnUiThread { webView.evaluateJavascript("window.OFH.onDone && window.OFH.onDone(${json(answer)});", null) }
                } catch (e: Exception) {
                    runOnUiThread { webView.evaluateJavascript("window.OFH.onDone && window.OFH.onDone(${json("Error: ${e.message}")});", null) }
                } finally {
                    busy = false
                }
            }.start()
        }

        @JavascriptInterface
        fun getProviders(): String {
            val arr = org.json.JSONArray()
            for (p in ProviderConfig.providers) {
                arr.put(JSONObject()
                    .put("route", p.route)
                    .put("label", p.label)
                    .put("defaultModel", p.defaultModel)
                    .put("needsKey", p.needsKey))
            }
            return arr.toString()
        }

        @JavascriptInterface
        fun getCurrent(): String {
            val c = ProviderConfig.readCurrent(this@WebViewActivity)
            return JSONObject().put("provider", c?.first ?: "").put("model", c?.second ?: "").toString()
        }

        @JavascriptInterface
        fun setProvider(route: String, model: String): String {
            val ok = ProviderConfig.setCurrent(this@WebViewActivity, route, model)
            return JSONObject().put("ok", ok).toString()
        }
    }

    private fun buildLoop(): AgentLoop {
        val current = ProviderConfig.readCurrent(this)
        val route = current?.first ?: "ollamacloud"
        val model = current?.second ?: ProviderConfig.preset(route).defaultModel
        val baseUrl = ProviderConfig.readBaseUrl(this, route)
        val key = ProviderConfig.readSavedKey(this, route)
        val adapter = ProviderAdapter(baseUrl, model, key)
        val registry = ToolRegistry()
        val memory = Memory.forContext(this)
        val skills = Skills.forContext(this)
        BuiltinTools.registerAll(registry, this, memory, skills)
        return AgentLoop(adapter, registry, memory = memory, skills = skills)
    }

    private fun json(s: String): String = JSONObject().put("v", s).toString()

    private fun toolJson(name: String, args: String, result: String): String =
        JSONObject().put("name", name).put("args", args).put("result", result).toString()
}
