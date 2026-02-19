package com.launcher.ai

import android.content.Context
import com.launcher.PREFS_NAME
import com.launcher.PREF_WORKER_SECRET
import com.launcher.PREF_WORKER_URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * HTTP client for communicating with the launcher-worker.
 *
 * The worker URL and secret are configured by the user in Settings and stored
 * in SharedPreferences. Users self-host the worker anywhere they choose —
 * local machine, Mac mini, GCP VM — and just paste the URL here.
 *
 * Worker providers (set via PROVIDER env var on the server):
 *   claude-cli        — Claude Code CLI (subscription, no extra API key)
 *   anthropic-api     — Anthropic API key
 *   ollama            — Local Ollama models
 *   openai-compatible — Any OpenAI-compatible server (OpenRouter, LM Studio, etc.)
 */
object AiWorkerClient {

    /**
     * Test the connection to the configured worker URL.
     * Returns a human-readable status string (success or error message).
     */
    suspend fun testConnection(url: String, secret: String): String = withContext(Dispatchers.IO) {
        try {
            val healthUrl = url.trimEnd('/') + "/health"
            val connection = URL(healthUrl).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.setRequestProperty("x-worker-secret", secret)

            val code = connection.responseCode
            if (code == 200) {
                val body = connection.inputStream.bufferedReader().readText()
                val json = JSONObject(body)
                val provider = json.optString("provider", "unknown")
                val model = json.optString("model", "")
                val modelPart = if (model.isNotBlank() && model != "claude-cli") " | $model" else ""
                "Connected: $provider$modelPart"
            } else {
                "Error: HTTP $code"
            }
        } catch (e: java.net.ConnectException) {
            "Failed: Could not reach $url — is the worker running?"
        } catch (e: java.net.SocketTimeoutException) {
            "Failed: Connection timed out"
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }

    /**
     * Send a chat message to the worker and stream the response via SSE.
     *
     * @param context Android context (to read SharedPreferences)
     * @param message The user's message
     * @param workerContext Launcher context (current app, clipboard, etc.)
     * @param userId User identifier for session management
     * @param sessionId Session identifier (conversation thread)
     * @param onChunk Called for each streamed text chunk
     * @param onDone Called when the stream completes
     * @param onError Called if an error occurs
     */
    suspend fun sendChat(
        context: Context,
        message: String,
        workerContext: Map<String, Any?> = emptyMap(),
        userId: String = "user",
        sessionId: String = "default",
        onChunk: (String) -> Unit,
        onDone: () -> Unit,
        onError: (String) -> Unit
    ) = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val workerUrl = prefs.getString(PREF_WORKER_URL, "")?.trimEnd('/') ?: ""
        val secret = prefs.getString(PREF_WORKER_SECRET, "") ?: ""

        if (workerUrl.isEmpty()) {
            onError("No AI Worker URL configured. Go to Settings → AI Assistant to set it up.")
            return@withContext
        }

        try {
            val chatUrl = "$workerUrl/chat"
            val connection = URL(chatUrl).openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.connectTimeout = 10000
            connection.readTimeout = 60000
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("x-worker-secret", secret)
            connection.setRequestProperty("Accept", "text/event-stream")

            val body = JSONObject().apply {
                put("userId", userId)
                put("sessionId", sessionId)
                put("message", message)
                put("context", JSONObject(workerContext))
            }.toString()

            connection.outputStream.use { it.write(body.toByteArray()) }

            val responseCode = connection.responseCode
            if (responseCode != 200) {
                onError("Worker returned HTTP $responseCode")
                return@withContext
            }

            // Parse SSE stream
            BufferedReader(InputStreamReader(connection.inputStream)).use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    val raw = line ?: continue
                    if (!raw.startsWith("data: ")) continue
                    val data = raw.removePrefix("data: ").trim()
                    if (data.isEmpty()) continue

                    try {
                        val event = JSONObject(data)
                        when (event.optString("type")) {
                            "chunk" -> onChunk(event.optString("text", ""))
                            "done" -> onDone()
                            "error" -> onError(event.optString("message", "Unknown error"))
                        }
                    } catch (e: Exception) {
                        // Ignore malformed SSE lines
                    }
                }
            }
        } catch (e: java.net.ConnectException) {
            onError("Could not connect to worker. Is it running at $workerUrl?")
        } catch (e: Exception) {
            onError("Error: ${e.message}")
        }
    }
}
