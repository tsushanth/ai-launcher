package com.launcher.extensions

import android.util.Log
import java.io.File

/**
 * Loads launcher extensions from files.
 *
 * Current implementation uses a registry pattern for simplicity.
 * Future versions can use Kotlin scripting for dynamic loading.
 */
class ExtensionLoader {
    private val extensionRegistry = mutableMapOf<String, () -> LauncherExtension>()

    init {
        // Register built-in sample extensions
        registerSampleExtensions()
    }

    /**
     * Load an extension from a file.
     *
     * Currently uses the registry to look up extensions by filename.
     * Future: Parse and compile Kotlin scripts at runtime.
     *
     * @param file Extension file (.kts)
     * @return Loaded extension or null if failed
     */
    fun loadFromFile(file: File): LauncherExtension? {
        return try {
            // Extract extension ID from filename (e.g., "com.launcher.weather.kts" -> "com.launcher.weather")
            val extensionId = file.nameWithoutExtension

            // Look up in registry
            val factory = extensionRegistry[extensionId]
            if (factory != null) {
                factory()
            } else {
                Log.w(TAG, "Extension $extensionId not found in registry")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load extension from ${file.name}", e)
            null
        }
    }

    /**
     * Register an extension factory.
     *
     * This allows pre-compiled extensions to be registered and loaded.
     *
     * @param extensionId Unique extension ID
     * @param factory Factory function that creates an instance of the extension
     */
    fun registerExtension(extensionId: String, factory: () -> LauncherExtension) {
        extensionRegistry[extensionId] = factory
        Log.d(TAG, "Registered extension: $extensionId")
    }

    /**
     * Register sample/built-in extensions.
     */
    private fun registerSampleExtensions() {
        // Register the calculator extension
        registerExtension("com.launcher.calculator") {
            CalculatorExtension()
        }

        // Register the weather extension (placeholder)
        registerExtension("com.launcher.weather") {
            WeatherExtension()
        }

        // Register the notes extension (placeholder)
        registerExtension("com.launcher.notes") {
            NotesExtension()
        }
    }

    companion object {
        private const val TAG = "ExtensionLoader"
    }
}

// ===== Sample Extensions =====

/**
 * Sample calculator extension.
 *
 * Responds to math queries like "calculate 2+2" or "what's 5*6".
 */
class CalculatorExtension : LauncherExtension {
    override val id = "com.launcher.calculator"
    override val name = "Calculator"
    override val version = "1.0.0"
    override val author = "AI Launcher Team"
    override val description = "Performs simple calculations"

    override fun onAIQuery(query: String, context: LauncherContext): ExtensionResponse? {
        val lowerQuery = query.lowercase()

        // Check if query is asking for a calculation
        if (!lowerQuery.contains("calculate") &&
            !lowerQuery.contains("what's") &&
            !lowerQuery.contains("what is") &&
            !lowerQuery.contains("+") &&
            !lowerQuery.contains("-") &&
            !lowerQuery.contains("*") &&
            !lowerQuery.contains("/")
        ) {
            return null
        }

        // Extract the math expression
        val expression = extractMathExpression(query) ?: return null

        // Evaluate the expression
        val result = evaluateExpression(expression) ?: return null

        return ExtensionResponse(
            text = "$expression = $result",
            priority = 10 // High priority for math queries
        )
    }

    private fun extractMathExpression(query: String): String? {
        // Simple regex to extract math expressions
        val pattern = Regex("""(\d+\.?\d*)\s*([+\-*/])\s*(\d+\.?\d*)""")
        val match = pattern.find(query) ?: return null
        return match.value
    }

    private fun evaluateExpression(expression: String): Double? {
        return try {
            val parts = expression.split(Regex("""([+\-*/])"""))
            if (parts.size != 3) return null

            val a = parts[0].trim().toDoubleOrNull() ?: return null
            val operator = expression.find { it in "+-*/" } ?: return null
            val b = parts[2].trim().toDoubleOrNull() ?: return null

            when (operator) {
                '+' -> a + b
                '-' -> a - b
                '*' -> a * b
                '/' -> if (b != 0.0) a / b else null
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }
}

/**
 * Sample weather extension (placeholder).
 */
class WeatherExtension : LauncherExtension {
    override val id = "com.launcher.weather"
    override val name = "Weather"
    override val version = "1.0.0"
    override val author = "AI Launcher Team"
    override val description = "Shows current weather information"
    override val permissions = listOf(ExtensionPermission.ACCESS_LOCATION, ExtensionPermission.NETWORK_ACCESS)

    override fun onAIQuery(query: String, context: LauncherContext): ExtensionResponse? {
        val lowerQuery = query.lowercase()

        if (!lowerQuery.contains("weather") && !lowerQuery.contains("temperature")) {
            return null
        }

        // TODO: Actually fetch weather data
        // For now, return a placeholder response
        return ExtensionResponse(
            text = "Weather extension is installed but not configured. Connect to a weather API to get real data.",
            actions = listOf(
                ExtensionAction.OpenSettings("weather_settings")
            ),
            priority = 8
        )
    }

    override fun provideWidget(): ExtensionWidget {
        return object : ExtensionWidget {
            override val id = "com.launcher.weather.widget"
            override val name = "Weather Widget"
            override val description = "Shows current weather"
            override val defaultSize = WidgetSize(2, 2)

            @androidx.compose.runtime.Composable
            override fun render() {
                // TODO: Implement weather widget UI
                androidx.compose.material3.Text("Weather Widget (Coming Soon)")
            }
        }
    }
}

/**
 * Sample notes extension (placeholder).
 */
class NotesExtension : LauncherExtension {
    override val id = "com.launcher.notes"
    override val name = "Quick Notes"
    override val version = "1.0.0"
    override val author = "AI Launcher Team"
    override val description = "Quick note-taking from the launcher"

    private val notes = mutableListOf<String>()

    override fun onAIQuery(query: String, context: LauncherContext): ExtensionResponse? {
        val lowerQuery = query.lowercase()

        // Check if user wants to create a note
        if (lowerQuery.startsWith("note:") || lowerQuery.startsWith("remember:")) {
            val noteText = query.substringAfter(':').trim()
            notes.add(noteText)

            return ExtensionResponse(
                text = "Note saved: \"$noteText\"",
                priority = 9
            )
        }

        // Check if user wants to see notes
        if (lowerQuery.contains("show notes") || lowerQuery.contains("my notes")) {
            val notesList = if (notes.isEmpty()) {
                "No notes saved yet."
            } else {
                notes.joinToString("\n") { "• $it" }
            }

            return ExtensionResponse(
                text = "Your notes:\n$notesList",
                priority = 9
            )
        }

        return null
    }
}
