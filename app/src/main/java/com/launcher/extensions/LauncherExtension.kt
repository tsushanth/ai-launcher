package com.launcher.extensions

import com.launcher.data.models.GridPosition

/**
 * Base interface for all launcher extensions.
 *
 * Extensions can hook into launcher events, respond to AI queries,
 * and contribute UI elements like widgets, search providers, and themes.
 */
interface LauncherExtension {
    /** Unique identifier for this extension (e.g., "com.launcher.weather") */
    val id: String

    /** Display name shown to users */
    val name: String

    /** Extension version (semantic versioning recommended) */
    val version: String

    /** Extension author/developer name */
    val author: String

    /** Short description of what the extension does */
    val description: String

    /** Required permissions (optional) */
    val permissions: List<ExtensionPermission> get() = emptyList()

    // ===== Lifecycle Hooks =====

    /**
     * Called when the extension is first installed.
     * Use this to set up any initial state or databases.
     */
    fun onInstall() {}

    /**
     * Called when the extension is enabled by the user.
     */
    fun onEnable() {}

    /**
     * Called when the extension is disabled by the user.
     */
    fun onDisable() {}

    /**
     * Called when the extension is uninstalled.
     * Use this to clean up any resources or databases.
     */
    fun onUninstall() {}

    // ===== Event Hooks =====

    /**
     * Called when the launcher starts.
     */
    fun onLauncherStart() {}

    /**
     * Called when an app is launched from the launcher.
     * @param packageName The package name of the launched app
     */
    fun onAppLaunched(packageName: String) {}

    /**
     * Called when the app drawer is opened.
     */
    fun onAppDrawerOpened() {}

    /**
     * Called when the user long-presses on an empty space on the home screen.
     * @param position The grid position that was long-pressed
     */
    fun onHomeScreenLongPress(position: GridPosition) {}

    // ===== AI Integration =====

    /**
     * Called when the user sends a query to the AI assistant.
     *
     * Return an ExtensionResponse if your extension can handle this query,
     * or null to let other extensions or the AI handle it.
     *
     * @param query The user's query text
     * @param context Current launcher context (apps, clipboard, etc.)
     * @return ExtensionResponse if handled, null otherwise
     */
    fun onAIQuery(query: String, context: LauncherContext): ExtensionResponse? = null

    // ===== UI Contributions =====

    /**
     * Provide a widget that can be added to the home screen.
     * @return Widget instance or null if no widget provided
     */
    fun provideWidget(): ExtensionWidget? = null

    /**
     * Provide a search provider for the app drawer search.
     * @return SearchProvider instance or null if no search provider
     */
    fun provideSearchProvider(): SearchProvider? = null

    /**
     * Provide a custom theme.
     * @return Theme instance or null if no theme provided
     */
    fun provideTheme(): ExtensionTheme? = null
}

/**
 * Response from an extension to an AI query.
 */
data class ExtensionResponse(
    /** Text response to show to the user */
    val text: String? = null,

    /** Actions that can be taken (buttons/links) */
    val actions: List<ExtensionAction> = emptyList(),

    /** Priority (higher = shown first, default 0) */
    val priority: Int = 0,

    /** Optional metadata for rendering */
    val metadata: Map<String, Any> = emptyMap()
)

/**
 * Actions that extensions can trigger.
 */
sealed class ExtensionAction {
    /** Launch an app by package name */
    data class LaunchApp(val packageName: String) : ExtensionAction()

    /** Open a URL in the browser */
    data class OpenUrl(val url: String) : ExtensionAction()

    /** Show a toast message */
    data class ShowToast(val message: String) : ExtensionAction()

    /** Create a reminder/notification */
    data class CreateReminder(val text: String, val timeMillis: Long) : ExtensionAction()

    /** Send a message (SMS or messaging app) */
    data class SendMessage(val contact: String, val message: String) : ExtensionAction()

    /** Open phone dialer */
    data class MakeCall(val phoneNumber: String) : ExtensionAction()

    /** Open settings */
    data class OpenSettings(val settingsAction: String? = null) : ExtensionAction()

    /** Custom action with intent data */
    data class CustomIntent(val action: String, val data: Map<String, String> = emptyMap()) : ExtensionAction()
}

/**
 * Context provided to extensions.
 */
data class LauncherContext(
    /** Currently focused app package name (if available) */
    val currentApp: String? = null,

    /** Recent notifications (if permission granted) */
    val recentNotifications: List<NotificationInfo> = emptyList(),

    /** Clipboard content (if available) */
    val clipboard: String? = null,

    /** List of installed app package names */
    val installedApps: List<String> = emptyList(),

    /** Current time in milliseconds */
    val currentTimeMillis: Long = System.currentTimeMillis(),

    /** Additional metadata */
    val metadata: Map<String, Any> = emptyMap()
)

/**
 * Notification info for context.
 */
data class NotificationInfo(
    val appPackage: String,
    val title: String?,
    val text: String?,
    val timestamp: Long
)

/**
 * Widget provided by an extension.
 */
interface ExtensionWidget {
    /** Unique ID for this widget */
    val id: String

    /** Widget display name */
    val name: String

    /** Widget description */
    val description: String

    /** Default size (in grid cells) */
    val defaultSize: WidgetSize

    /** Whether the widget can be resized */
    val resizable: Boolean get() = true

    /**
     * Render the widget content.
     * This will be called by the launcher to display the widget.
     */
    @androidx.compose.runtime.Composable
    fun render()
}

/**
 * Widget size in grid cells.
 */
data class WidgetSize(val width: Int, val height: Int)

/**
 * Search provider for app drawer.
 */
interface SearchProvider {
    /** Unique ID for this search provider */
    val id: String

    /** Provider name */
    val name: String

    /**
     * Search for items matching the query.
     * @param query Search query from user
     * @return List of search results
     */
    suspend fun search(query: String): List<SearchResult>
}

/**
 * Search result from a provider.
 */
data class SearchResult(
    val id: String,
    val title: String,
    val subtitle: String? = null,
    val iconUrl: String? = null,
    val action: ExtensionAction
)

/**
 * Theme provided by an extension.
 */
interface ExtensionTheme {
    /** Unique ID for this theme */
    val id: String

    /** Theme name */
    val name: String

    /** Theme description */
    val description: String

    /** Color scheme */
    val colors: ThemeColors
}

/**
 * Color scheme for a theme.
 */
data class ThemeColors(
    val primary: Long, // Color as ARGB Long
    val accent: Long,
    val background: Long,
    val surface: Long,
    val text: Long,
    val textSecondary: Long
)

/**
 * Permissions that extensions can request.
 */
enum class ExtensionPermission {
    /** Read clipboard content */
    READ_CLIPBOARD,

    /** Read notifications */
    READ_NOTIFICATIONS,

    /** Read calendar events */
    READ_CALENDAR,

    /** Read contacts */
    READ_CONTACTS,

    /** Access location */
    ACCESS_LOCATION,

    /** Send notifications */
    SEND_NOTIFICATIONS,

    /** Access network */
    NETWORK_ACCESS,

    /** Read app usage stats */
    APP_USAGE_STATS
}
