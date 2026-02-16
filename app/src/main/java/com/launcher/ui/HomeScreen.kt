package com.launcher.ui

import android.content.Intent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.viewmodel.compose.viewModel
import com.launcher.data.models.AppInfo
import com.launcher.data.models.DesktopItem
import com.launcher.ui.viewmodels.HomeScreenViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    viewModel: HomeScreenViewModel = viewModel(),
    onOpenAppDrawer: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val context = LocalContext.current
    val dockApps by viewModel.dockApps.collectAsState()
    val desktopItems by viewModel.desktopItems.collectAsState()
    val layout by viewModel.homeScreenLayout.collectAsState()

    var selectedFolder by remember { mutableStateOf<DesktopItem.Folder?>(null) }
    var showWallpaperPicker by remember { mutableStateOf(false) }
    var longPressedItem by remember { mutableStateOf<LongPressItem?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .pointerInput(Unit) {
                detectVerticalDragGestures { _, dragAmount ->
                    // Swipe up to open app drawer
                    if (dragAmount < -50) {
                        onOpenAppDrawer()
                    }
                }
            }
            .combinedClickable(
                onClick = {},
                onLongClick = {
                    // Long press on empty space
                    longPressedItem = LongPressItem.EmptySpace
                }
            )
    ) {
        // Main home screen content
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Spacer(Modifier.height(48.dp))

            // Search bar at top
            SearchBar(
                onClick = onOpenAppDrawer,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )

            Spacer(Modifier.height(24.dp))

            // Desktop grid with apps and folders
            DesktopGrid(
                items = desktopItems,
                gridRows = layout.gridRows,
                gridCols = layout.gridCols,
                onItemClick = { item ->
                    when (item) {
                        is DesktopItem.AppShortcut -> {
                            val intent = context.packageManager.getLaunchIntentForPackage(item.packageName)
                            intent?.let {
                                context.startActivity(it)
                                viewModel.notifyAppLaunched(item.packageName)
                            }
                        }
                        is DesktopItem.Folder -> {
                            selectedFolder = item
                        }
                        is DesktopItem.Widget -> {
                            // Widget interactions handled by widget itself
                        }
                    }
                },
                onItemLongPress = { item ->
                    longPressedItem = when (item) {
                        is DesktopItem.AppShortcut -> LongPressItem.App(item.packageName)
                        is DesktopItem.Folder -> LongPressItem.Folder(item)
                        is DesktopItem.Widget -> LongPressItem.Widget(item.widgetId)
                    }
                },
                getAppInfo = { packageName -> viewModel.getAppInfo(packageName) },
                getAppsForFolder = { folder -> viewModel.getAppsForFolder(folder) },
                modifier = Modifier.weight(1f)
            )

            // Dock at bottom
            Dock(
                apps = dockApps,
                onAppClick = { app ->
                    val intent = context.packageManager.getLaunchIntentForPackage(app.packageName)
                    intent?.let {
                        context.startActivity(it)
                        viewModel.notifyAppLaunched(app.packageName)
                    }
                },
                onAppLongPress = { app ->
                    longPressedItem = LongPressItem.DockApp(app.packageName)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )

            Spacer(Modifier.height(24.dp))
        }

        // Settings button (top right)
        IconButton(
            onClick = onOpenSettings,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            Icon(
                Icons.Default.Settings,
                contentDescription = "Settings",
                tint = MaterialTheme.colorScheme.onBackground
            )
        }
    }

    // Folder dialog
    selectedFolder?.let { folder ->
        val apps = viewModel.getAppsForFolder(folder)
        FolderDialog(
            folderName = folder.name,
            apps = apps,
            onDismiss = { selectedFolder = null },
            onAppClick = { app ->
                val intent = context.packageManager.getLaunchIntentForPackage(app.packageName)
                intent?.let {
                    context.startActivity(it)
                    viewModel.notifyAppLaunched(app.packageName)
                    selectedFolder = null
                }
            },
            onRename = { newName ->
                viewModel.renameFolder(folder.id, newName)
            },
            onRemoveApp = { app ->
                viewModel.removeAppFromFolder(folder.id, app.packageName)
            }
        )
    }

    // Wallpaper picker
    if (showWallpaperPicker) {
        WallpaperPickerDialog(
            onDismiss = { showWallpaperPicker = false }
        )
    }

    // Long press menu
    longPressedItem?.let { item ->
        LongPressMenu(
            item = item,
            onDismiss = { longPressedItem = null },
            onRemove = {
                when (item) {
                    is LongPressItem.App -> viewModel.removeAppFromHomeScreen(item.packageName)
                    is LongPressItem.Folder -> {
                        // Remove folder (repository handles cleanup)
                        item.folder.apps.forEach { packageName ->
                            viewModel.removeAppFromFolder(item.folder.id, packageName)
                        }
                    }
                    is LongPressItem.Widget -> {
                        // Remove widget (TODO: implement widget removal)
                    }
                    else -> {}
                }
                longPressedItem = null
            },
            onChangeWallpaper = {
                showWallpaperPicker = true
                longPressedItem = null
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DesktopGrid(
    items: List<DesktopItem>,
    gridRows: Int,
    gridCols: Int,
    onItemClick: (DesktopItem) -> Unit,
    onItemLongPress: (DesktopItem) -> Unit,
    getAppInfo: (String) -> AppInfo?,
    getAppsForFolder: (DesktopItem.Folder) -> List<AppInfo>,
    modifier: Modifier = Modifier
) {
    // For now, show items in a simple vertical list
    // TODO: Implement proper grid positioning based on GridPosition
    Column(
        modifier = modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items.forEach { item ->
            when (item) {
                is DesktopItem.AppShortcut -> {
                    getAppInfo(item.packageName)?.let { app ->
                        AppIcon(
                            app = app,
                            onClick = { onItemClick(item) },
                            onLongClick = { onItemLongPress(item) },
                            size = 60.dp,
                            showLabel = true
                        )
                    }
                }
                is DesktopItem.Folder -> {
                    val apps = getAppsForFolder(item)
                    FolderIcon(
                        name = item.name,
                        apps = apps,
                        onClick = { onItemClick(item) },
                        modifier = Modifier.combinedClickable(
                            onClick = { onItemClick(item) },
                            onLongClick = { onItemLongPress(item) }
                        )
                    )
                }
                is DesktopItem.Widget -> {
                    // Widget rendering (simplified)
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .combinedClickable(
                                onClick = {},
                                onLongClick = { onItemLongPress(item) }
                            ),
                        shape = RoundedCornerShape(16.dp),
                        tonalElevation = 2.dp
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Widget ${item.widgetId}")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SearchBar(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(56.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Search,
                contentDescription = "Search",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.width(16.dp))

            Text(
                text = "Search apps, contacts, web…",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Dock(
    apps: List<AppInfo>,
    onAppClick: (AppInfo) -> Unit,
    onAppLongPress: (AppInfo) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.height(96.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
        tonalElevation = 3.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            apps.take(5).forEach { app ->
                AppIcon(
                    app = app,
                    onClick = { onAppClick(app) },
                    onLongClick = { onAppLongPress(app) },
                    size = 56.dp,
                    showLabel = false
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppIcon(
    app: AppInfo,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    size: androidx.compose.ui.unit.Dp = 60.dp,
    showLabel: Boolean = true,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(size + 16.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // App icon
        Surface(
            modifier = Modifier.size(size),
            shape = CircleShape,
            tonalElevation = 1.dp
        ) {
            val bitmap = app.icon.toBitmap(
                width = size.value.toInt(),
                height = size.value.toInt()
            )
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = app.name,
                modifier = Modifier.fillMaxSize()
            )
        }

        if (showLabel) {
            Spacer(Modifier.height(8.dp))

            Text(
                text = app.name,
                style = MaterialTheme.typography.bodySmall,
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(size + 8.dp)
            )
        }
    }
}

// Long press menu item types
sealed class LongPressItem {
    data class App(val packageName: String) : LongPressItem()
    data class DockApp(val packageName: String) : LongPressItem()
    data class Folder(val folder: DesktopItem.Folder) : LongPressItem()
    data class Widget(val widgetId: Int) : LongPressItem()
    object EmptySpace : LongPressItem()
}

@Composable
fun LongPressMenu(
    item: LongPressItem,
    onDismiss: () -> Unit,
    onRemove: () -> Unit,
    onChangeWallpaper: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                when (item) {
                    is LongPressItem.App -> "App Options"
                    is LongPressItem.DockApp -> "Dock App"
                    is LongPressItem.Folder -> "Folder: ${item.folder.name}"
                    is LongPressItem.Widget -> "Widget Options"
                    is LongPressItem.EmptySpace -> "Home Screen"
                }
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                when (item) {
                    is LongPressItem.App, is LongPressItem.DockApp -> {
                        MenuOption(
                            icon = Icons.Default.Delete,
                            text = "Remove",
                            onClick = onRemove
                        )
                        MenuOption(
                            icon = Icons.Default.Info,
                            text = "App Info",
                            onClick = { /* TODO: Open app info */ }
                        )
                    }
                    is LongPressItem.Folder -> {
                        MenuOption(
                            icon = Icons.Default.Delete,
                            text = "Delete Folder",
                            onClick = onRemove
                        )
                    }
                    is LongPressItem.Widget -> {
                        MenuOption(
                            icon = Icons.Default.Delete,
                            text = "Remove Widget",
                            onClick = onRemove
                        )
                    }
                    is LongPressItem.EmptySpace -> {
                        MenuOption(
                            icon = Icons.Default.Wallpaper,
                            text = "Change Wallpaper",
                            onClick = onChangeWallpaper
                        )
                        MenuOption(
                            icon = Icons.Default.Widgets,
                            text = "Add Widget",
                            onClick = { /* TODO: Open widget picker */ }
                        )
                    }
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
private fun MenuOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = text)
            Spacer(Modifier.width(16.dp))
            Text(text)
        }
    }
}
