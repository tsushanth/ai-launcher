package com.launcher.extensions

import android.content.Context
import android.util.Log
import com.launcher.data.models.GridPosition
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

/**
 * Manages all launcher extensions.
 *
 * Handles:
 * - Loading extensions from files
 * - Enabling/disabling extensions
 * - Dispatching events to extensions
 * - Querying extensions for AI responses
 */
class ExtensionManager(private val context: Context) {
    private val _loadedExtensions = MutableStateFlow<Map<String, LauncherExtension>>(emptyMap())
    val loadedExtensions: StateFlow<Map<String, LauncherExtension>> = _loadedExtensions.asStateFlow()

    private val _enabledExtensions = MutableStateFlow<Set<String>>(emptySet())
    val enabledExtensions: StateFlow<Set<String>> = _enabledExtensions.asStateFlow()

    private val extensionsDir = File(context.filesDir, "extensions")
    private val extensionLoader = ExtensionLoader()

    init {
        extensionsDir.mkdirs()
    }

    // ===== Installation =====

    /**
     * Install an extension from a file.
     *
     * @param extensionFile File containing the extension code
     * @return Result with extension ID if successful
     */
    suspend fun installExtension(extensionFile: File): Result<String> {
        return try {
            // Load the extension
            val extension = extensionLoader.loadFromFile(extensionFile)
                ?: return Result.failure(Exception("Failed to load extension"))

            // Validate extension
            if (extension.id.isBlank()) {
                return Result.failure(Exception("Extension ID cannot be blank"))
            }

            // Check if already installed
            if (_loadedExtensions.value.containsKey(extension.id)) {
                return Result.failure(Exception("Extension ${extension.id} is already installed"))
            }

            // Copy extension file to extensions directory
            val installedFile = File(extensionsDir, "${extension.id}.kts")
            extensionFile.copyTo(installedFile, overwrite = true)

            // Add to loaded extensions
            _loadedExtensions.value = _loadedExtensions.value + (extension.id to extension)

            // Call onInstall
            extension.onInstall()

            Log.i(TAG, "Installed extension: ${extension.id} (${extension.name})")

            Result.success(extension.id)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to install extension", e)
            Result.failure(e)
        }
    }

    /**
     * Uninstall an extension.
     *
     * @param extensionId ID of the extension to uninstall
     */
    suspend fun uninstallExtension(extensionId: String): Result<Unit> {
        return try {
            val extension = _loadedExtensions.value[extensionId]
                ?: return Result.failure(Exception("Extension $extensionId not found"))

            // Disable first if enabled
            if (_enabledExtensions.value.contains(extensionId)) {
                disableExtension(extensionId)
            }

            // Call onUninstall
            extension.onUninstall()

            // Remove from loaded extensions
            _loadedExtensions.value = _loadedExtensions.value - extensionId

            // Delete file
            val extensionFile = File(extensionsDir, "$extensionId.kts")
            extensionFile.delete()

            Log.i(TAG, "Uninstalled extension: $extensionId")

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to uninstall extension $extensionId", e)
            Result.failure(e)
        }
    }

    // ===== Enable/Disable =====

    /**
     * Enable an extension.
     */
    fun enableExtension(extensionId: String) {
        val extension = _loadedExtensions.value[extensionId] ?: return

        if (_enabledExtensions.value.contains(extensionId)) {
            return // Already enabled
        }

        _enabledExtensions.value = _enabledExtensions.value + extensionId
        extension.onEnable()

        Log.i(TAG, "Enabled extension: $extensionId")
    }

    /**
     * Disable an extension.
     */
    fun disableExtension(extensionId: String) {
        val extension = _loadedExtensions.value[extensionId] ?: return

        if (!_enabledExtensions.value.contains(extensionId)) {
            return // Already disabled
        }

        _enabledExtensions.value = _enabledExtensions.value - extensionId
        extension.onDisable()

        Log.i(TAG, "Disabled extension: $extensionId")
    }

    /**
     * Check if an extension is enabled.
     */
    fun isEnabled(extensionId: String): Boolean {
        return _enabledExtensions.value.contains(extensionId)
    }

    // ===== Event Dispatching =====

    /**
     * Dispatch launcher start event to all enabled extensions.
     */
    fun onLauncherStart() {
        getEnabledExtensions().forEach { extension ->
            try {
                extension.onLauncherStart()
            } catch (e: Exception) {
                Log.e(TAG, "Extension ${extension.id} onLauncherStart failed", e)
            }
        }
    }

    /**
     * Dispatch app launched event to all enabled extensions.
     */
    fun onAppLaunched(packageName: String) {
        getEnabledExtensions().forEach { extension ->
            try {
                extension.onAppLaunched(packageName)
            } catch (e: Exception) {
                Log.e(TAG, "Extension ${extension.id} onAppLaunched failed", e)
            }
        }
    }

    /**
     * Dispatch app drawer opened event to all enabled extensions.
     */
    fun onAppDrawerOpened() {
        getEnabledExtensions().forEach { extension ->
            try {
                extension.onAppDrawerOpened()
            } catch (e: Exception) {
                Log.e(TAG, "Extension ${extension.id} onAppDrawerOpened failed", e)
            }
        }
    }

    /**
     * Dispatch home screen long press event to all enabled extensions.
     */
    fun onHomeScreenLongPress(position: GridPosition) {
        getEnabledExtensions().forEach { extension ->
            try {
                extension.onHomeScreenLongPress(position)
            } catch (e: Exception) {
                Log.e(TAG, "Extension ${extension.id} onHomeScreenLongPress failed", e)
            }
        }
    }

    // ===== AI Integration =====

    /**
     * Query all enabled extensions for AI responses.
     *
     * @param query User's query
     * @param context Launcher context
     * @return List of responses from extensions, sorted by priority
     */
    suspend fun queryExtensions(query: String, context: LauncherContext): List<ExtensionResponse> {
        val responses = mutableListOf<ExtensionResponse>()

        getEnabledExtensions().forEach { extension ->
            try {
                val response = extension.onAIQuery(query, context)
                if (response != null) {
                    responses.add(response)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Extension ${extension.id} onAIQuery failed", e)
            }
        }

        // Sort by priority (highest first)
        return responses.sortedByDescending { it.priority }
    }

    // ===== UI Contributions =====

    /**
     * Get all widgets provided by enabled extensions.
     */
    fun getExtensionWidgets(): List<ExtensionWidget> {
        return getEnabledExtensions().mapNotNull { extension ->
            try {
                extension.provideWidget()
            } catch (e: Exception) {
                Log.e(TAG, "Extension ${extension.id} provideWidget failed", e)
                null
            }
        }
    }

    /**
     * Get all search providers from enabled extensions.
     */
    fun getSearchProviders(): List<SearchProvider> {
        return getEnabledExtensions().mapNotNull { extension ->
            try {
                extension.provideSearchProvider()
            } catch (e: Exception) {
                Log.e(TAG, "Extension ${extension.id} provideSearchProvider failed", e)
                null
            }
        }
    }

    /**
     * Get all themes from enabled extensions.
     */
    fun getExtensionThemes(): List<ExtensionTheme> {
        return getEnabledExtensions().mapNotNull { extension ->
            try {
                extension.provideTheme()
            } catch (e: Exception) {
                Log.e(TAG, "Extension ${extension.id} provideTheme failed", e)
                null
            }
        }
    }

    // ===== Loading =====

    /**
     * Load all installed extensions from the extensions directory.
     */
    suspend fun loadInstalledExtensions() {
        if (!extensionsDir.exists()) {
            extensionsDir.mkdirs()
            return
        }

        val extensionFiles = extensionsDir.listFiles { file ->
            file.isFile && file.extension == "kts"
        } ?: return

        extensionFiles.forEach { file ->
            try {
                val extension = extensionLoader.loadFromFile(file)
                if (extension != null) {
                    _loadedExtensions.value = _loadedExtensions.value + (extension.id to extension)
                    Log.i(TAG, "Loaded extension: ${extension.id} (${extension.name})")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load extension from ${file.name}", e)
            }
        }
    }

    // ===== Helpers =====

    private fun getEnabledExtensions(): List<LauncherExtension> {
        return _enabledExtensions.value.mapNotNull { id ->
            _loadedExtensions.value[id]
        }
    }

    companion object {
        private const val TAG = "ExtensionManager"
    }
}
