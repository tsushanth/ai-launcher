package com.launcher.data.models

import androidx.compose.ui.graphics.Color
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Theme data model.
 *
 * Represents a visual theme that can be applied to the launcher.
 */
@Entity(tableName = "themes")
data class Theme(
    @PrimaryKey val id: String,
    val name: String,
    val description: String = "",
    val author: String = "System",

    // Color scheme
    val primaryColor: Long,
    val accentColor: Long,
    val backgroundColor: Long,
    val surfaceColor: Long,
    val textColor: Long,
    val textSecondaryColor: Long,

    // Icon configuration
    val iconStyle: IconStyle = IconStyle.ROUNDED,
    val iconColorMode: IconColorMode = IconColorMode.ORIGINAL,

    // Wallpaper
    val wallpaperType: WallpaperType = WallpaperType.NONE,
    val wallpaperData: String? = null, // URI or gradient colors JSON

    // Metadata
    val occasion: String? = null, // birthday, halloween, christmas, etc.
    val aiGenerated: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val isBuiltIn: Boolean = false,
    val isActive: Boolean = false
)

/**
 * Icon style options.
 */
enum class IconStyle {
    ROUNDED,    // Rounded corners (default)
    SQUARE,     // Sharp corners
    SQUIRCLE,   // iOS-style squircle
    TEARDROP,   // Teardrop shape
    CIRCLE      // Circular
}

/**
 * Icon color mode.
 */
enum class IconColorMode {
    ORIGINAL,   // Keep original app icons
    MONOCHROME, // Make icons monochrome
    THEMED      // Apply theme colors to icons
}

/**
 * Wallpaper type.
 */
enum class WallpaperType {
    NONE,       // No wallpaper (use background color)
    STATIC,     // Static image
    GRADIENT,   // Gradient background
    LIVE,       // Live wallpaper
    VIDEO       // Video wallpaper
}

/**
 * Helper to convert Long (ARGB) to Compose Color.
 */
fun Long.toColor(): Color {
    return Color(this.toULong())
}

/**
 * Helper to convert Compose Color to Long (ARGB).
 */
fun Color.toLong(): Long {
    return this.value.toLong()
}

/**
 * Default built-in themes.
 */
object DefaultThemes {
    val LIGHT = Theme(
        id = "default_light",
        name = "Light",
        description = "Clean and minimal light theme",
        primaryColor = Color(0xFF1976D2).toLong(),
        accentColor = Color(0xFF2196F3).toLong(),
        backgroundColor = Color(0xFFFFFFFF).toLong(),
        surfaceColor = Color(0xFFF5F5F5).toLong(),
        textColor = Color(0xFF000000).toLong(),
        textSecondaryColor = Color(0xFF757575).toLong(),
        isBuiltIn = true
    )

    val DARK = Theme(
        id = "default_dark",
        name = "Dark",
        description = "Easy on the eyes dark theme",
        primaryColor = Color(0xFF90CAF9).toLong(),
        accentColor = Color(0xFF64B5F6).toLong(),
        backgroundColor = Color(0xFF121212).toLong(),
        surfaceColor = Color(0xFF1E1E1E).toLong(),
        textColor = Color(0xFFFFFFFF).toLong(),
        textSecondaryColor = Color(0xFFB0B0B0).toLong(),
        isBuiltIn = true
    )

    val AMOLED = Theme(
        id = "default_amoled",
        name = "AMOLED Black",
        description = "Pure black for OLED screens",
        primaryColor = Color(0xFF90CAF9).toLong(),
        accentColor = Color(0xFF64B5F6).toLong(),
        backgroundColor = Color(0xFF000000).toLong(),
        surfaceColor = Color(0xFF000000).toLong(),
        textColor = Color(0xFFFFFFFF).toLong(),
        textSecondaryColor = Color(0xFFB0B0B0).toLong(),
        isBuiltIn = true
    )

    val OCEAN = Theme(
        id = "default_ocean",
        name = "Ocean",
        description = "Cool ocean blues",
        primaryColor = Color(0xFF00ACC1).toLong(),
        accentColor = Color(0xFF26C6DA).toLong(),
        backgroundColor = Color(0xFF006064).toLong(),
        surfaceColor = Color(0xFF00838F).toLong(),
        textColor = Color(0xFFFFFFFF).toLong(),
        textSecondaryColor = Color(0xFFB2EBF2).toLong(),
        wallpaperType = WallpaperType.GRADIENT,
        wallpaperData = """{"colors":["#006064","#00838F","#00ACC1"]}""",
        isBuiltIn = true
    )

    val SUNSET = Theme(
        id = "default_sunset",
        name = "Sunset",
        description = "Warm sunset colors",
        primaryColor = Color(0xFFFF6F00).toLong(),
        accentColor = Color(0xFFFF9100).toLong(),
        backgroundColor = Color(0xFFBF360C).toLong(),
        surfaceColor = Color(0xFFE64A19).toLong(),
        textColor = Color(0xFFFFFFFF).toLong(),
        textSecondaryColor = Color(0xFFFFCCBC).toLong(),
        wallpaperType = WallpaperType.GRADIENT,
        wallpaperData = """{"colors":["#BF360C","#E64A19","#FF6F00"]}""",
        isBuiltIn = true
    )

    val FOREST = Theme(
        id = "default_forest",
        name = "Forest",
        description = "Natural forest greens",
        primaryColor = Color(0xFF388E3C).toLong(),
        accentColor = Color(0xFF66BB6A).toLong(),
        backgroundColor = Color(0xFF1B5E20).toLong(),
        surfaceColor = Color(0xFF2E7D32).toLong(),
        textColor = Color(0xFFFFFFFF).toLong(),
        textSecondaryColor = Color(0xFFC8E6C9).toLong(),
        wallpaperType = WallpaperType.GRADIENT,
        wallpaperData = """{"colors":["#1B5E20","#2E7D32","#388E3C"]}""",
        isBuiltIn = true
    )

    fun getAll(): List<Theme> = listOf(
        LIGHT,
        DARK,
        AMOLED,
        OCEAN,
        SUNSET,
        FOREST
    )
}
