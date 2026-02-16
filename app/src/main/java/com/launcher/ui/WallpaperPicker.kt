package com.launcher.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.launcher.utils.LauncherWallpaperManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WallpaperPickerDialog(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val wallpaperManager = remember { LauncherWallpaperManager(context) }
    val scope = rememberCoroutineScope()
    var showMessage by remember { mutableStateOf<String?>(null) }

    // Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                val result = wallpaperManager.setStaticWallpaper(it)
                showMessage = if (result.isSuccess) {
                    "Wallpaper set successfully"
                } else {
                    "Failed to set wallpaper: ${result.exceptionOrNull()?.message}"
                }
            }
        }
    }

    // Live wallpaper picker launcher
    val liveWallpaperLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        showMessage = "Live wallpaper selected"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Change Wallpaper") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Static wallpaper from gallery
                WallpaperOption(
                    icon = Icons.Default.Image,
                    title = "Choose from Gallery",
                    description = "Set a static wallpaper from your photos",
                    onClick = {
                        imagePickerLauncher.launch("image/*")
                    }
                )

                // Live wallpaper
                if (wallpaperManager.supportsLiveWallpapers()) {
                    WallpaperOption(
                        icon = Icons.Default.Animation,
                        title = "Live Wallpaper",
                        description = "Choose an animated wallpaper",
                        onClick = {
                            wallpaperManager.pickLiveWallpaper(liveWallpaperLauncher)
                        }
                    )
                }

                // Default wallpaper
                WallpaperOption(
                    icon = Icons.Default.WallpaperOutlined,
                    title = "Default Wallpaper",
                    description = "Reset to system default",
                    onClick = {
                        scope.launch {
                            val result = wallpaperManager.clearWallpaper()
                            showMessage = if (result.isSuccess) {
                                "Wallpaper reset to default"
                            } else {
                                "Failed to reset wallpaper"
                            }
                        }
                    }
                )

                // AI-generated wallpaper (placeholder)
                WallpaperOption(
                    icon = Icons.Default.AutoAwesome,
                    title = "AI-Generated",
                    description = "Coming soon: Generate with AI",
                    onClick = { showMessage = "AI wallpaper generation coming soon!" },
                    enabled = false
                )

                // Show status message
                showMessage?.let { message ->
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun WallpaperOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        enabled = enabled,
        shape = MaterialTheme.shapes.medium,
        tonalElevation = if (enabled) 1.dp else 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier.size(40.dp),
                tint = if (enabled) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                }
            )

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (enabled) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    }
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (enabled) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    }
                )
            }
        }
    }
}

// Missing icon placeholder
private val Icons.Default.WallpaperOutlined: androidx.compose.ui.graphics.vector.ImageVector
    get() = Icons.Default.Wallpaper

private val Icons.Default.Animation: androidx.compose.ui.graphics.vector.ImageVector
    get() = Icons.Default.Animation

private val Icons.Default.Image: androidx.compose.ui.graphics.vector.ImageVector
    get() = Icons.Default.Image
