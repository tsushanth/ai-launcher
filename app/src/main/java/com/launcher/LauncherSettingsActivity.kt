package com.launcher

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.launcher.ai.AiWorkerClient
import com.launcher.ui.ExtensionsScreen
import com.launcher.ui.theme.LauncherTheme
import kotlinx.coroutines.launch

const val PREFS_NAME = "launcher_prefs"
const val PREF_WORKER_URL = "ai_worker_url"
const val PREF_WORKER_SECRET = "ai_worker_secret"

class LauncherSettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            LauncherTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var showExtensions by remember { mutableStateOf(false) }

                    if (showExtensions) {
                        ExtensionsScreen(onBack = { showExtensions = false })
                    } else {
                        SettingsScreen(
                            onBack = { finish() },
                            onOpenExtensions = { showExtensions = true }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenExtensions: () -> Unit = {}
) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val scope = rememberCoroutineScope()

    var workerUrl by remember { mutableStateOf(prefs.getString(PREF_WORKER_URL, "") ?: "") }
    var workerSecret by remember { mutableStateOf(prefs.getString(PREF_WORKER_SECRET, "") ?: "") }
    var secretVisible by remember { mutableStateOf(false) }
    var connectionStatus by remember { mutableStateOf<String?>(null) }
    var testingConnection by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Launcher Settings") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            }
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // ── General ──────────────────────────────────────────────────────
            item {
                Text(
                    text = "General",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            item {
                SettingsCard {
                    Column {
                        Text("Grid Size", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "4 x 5 (Coming soon)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                SettingsCard {
                    Column {
                        Text("Icon Pack", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "Default (Coming soon)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // ── AI Assistant ─────────────────────────────────────────────────
            item {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "AI Assistant",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Connect to your own AI worker. Self-host on a Mac mini, GCP VM, or any server.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item {
                SettingsCard {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        // Worker URL field
                        OutlinedTextField(
                            value = workerUrl,
                            onValueChange = {
                                workerUrl = it
                                connectionStatus = null
                                prefs.edit().putString(PREF_WORKER_URL, it).apply()
                            },
                            label = { Text("AI Worker URL") },
                            placeholder = { Text("http://192.168.1.5:3456") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
                        )

                        // Worker secret field
                        OutlinedTextField(
                            value = workerSecret,
                            onValueChange = {
                                workerSecret = it
                                connectionStatus = null
                                prefs.edit().putString(PREF_WORKER_SECRET, it).apply()
                            },
                            label = { Text("Worker Secret") },
                            placeholder = { Text("your-worker-secret") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            visualTransformation = if (secretVisible)
                                VisualTransformation.None
                            else
                                PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            trailingIcon = {
                                IconButton(onClick = { secretVisible = !secretVisible }) {
                                    Icon(
                                        if (secretVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = if (secretVisible) "Hide" else "Show"
                                    )
                                }
                            }
                        )

                        // Connection status
                        connectionStatus?.let { status ->
                            val isError = status.startsWith("Error") || status.startsWith("Failed")
                            Text(
                                text = status,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isError)
                                    MaterialTheme.colorScheme.error
                                else
                                    MaterialTheme.colorScheme.primary
                            )
                        }

                        // Test Connection button
                        Button(
                            onClick = {
                                if (workerUrl.isBlank()) {
                                    Toast.makeText(context, "Enter a worker URL first", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                testingConnection = true
                                connectionStatus = "Testing connection..."
                                scope.launch {
                                    val result = AiWorkerClient.testConnection(workerUrl.trim(), workerSecret.trim())
                                    connectionStatus = result
                                    testingConnection = false
                                }
                            },
                            enabled = !testingConnection,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (testingConnection) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(Modifier.width(8.dp))
                            }
                            Text("Test Connection")
                        }
                    }
                }
            }

            item {
                SettingsCard {
                    Column {
                        Text("Worker Providers", style = MaterialTheme.typography.bodyLarge)
                        Spacer(Modifier.height(4.dp))
                        listOf(
                            "claude-cli" to "Claude Code CLI (subscription, no API key)",
                            "anthropic-api" to "Anthropic API (API key required)",
                            "ollama" to "Ollama local models (free, self-hosted)",
                            "openai-compatible" to "OpenRouter, LM Studio, or any OpenAI-compatible server"
                        ).forEach { (provider, desc) ->
                            Row(
                                modifier = Modifier.padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "•",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Column {
                                    Text(provider, style = MaterialTheme.typography.bodySmall)
                                    Text(
                                        desc,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Set PROVIDER in your worker's .env file",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // ── Extensions ───────────────────────────────────────────────────
            item {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Extensions",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            item {
                SettingsCard(onClick = onOpenExtensions) {
                    Column {
                        Text("Manage Extensions", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "Enable and configure launcher extensions",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // ── About ────────────────────────────────────────────────────────
            item {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "About",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            item {
                SettingsCard {
                    Column {
                        Text("Version", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "1.0.0 (Alpha)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsCard(
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Surface(
        onClick = onClick ?: {},
        modifier = Modifier.fillMaxWidth(),
        enabled = onClick != null,
        tonalElevation = 1.dp,
        shape = MaterialTheme.shapes.medium
    ) {
        Box(modifier = Modifier.padding(16.dp)) {
            content()
        }
    }
}
