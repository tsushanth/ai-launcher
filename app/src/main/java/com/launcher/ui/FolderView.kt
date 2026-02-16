package com.launcher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.launcher.data.models.AppInfo

/**
 * Folder icon displayed on home screen
 * Shows a grid preview of apps inside
 */
@Composable
fun FolderIcon(
    name: String,
    apps: List<AppInfo>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(76.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Folder icon background with app previews
        Surface(
            modifier = Modifier.size(60.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 2.dp
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                // Show up to 4 app icons in a 2x2 grid
                Row(
                    modifier = Modifier.padding(6.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        apps.getOrNull(0)?.let { app ->
                            MiniAppIcon(app)
                        }
                        apps.getOrNull(2)?.let { app ->
                            MiniAppIcon(app)
                        }
                    }
                    Column(
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        apps.getOrNull(1)?.let { app ->
                            MiniAppIcon(app)
                        }
                        apps.getOrNull(3)?.let { app ->
                            MiniAppIcon(app)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // Folder name
        Text(
            text = name.ifEmpty { "Unnamed" },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(68.dp)
        )
    }
}

@Composable
private fun MiniAppIcon(app: AppInfo) {
    Surface(
        modifier = Modifier.size(22.dp),
        shape = CircleShape,
        tonalElevation = 1.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
        )
        // TODO: Load actual app icon (simplified for now)
    }
}

/**
 * Full-screen folder dialog showing all apps in folder
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderDialog(
    folderName: String,
    apps: List<AppInfo>,
    onDismiss: () -> Unit,
    onAppClick: (AppInfo) -> Unit,
    onRename: (String) -> Unit,
    onRemoveApp: (AppInfo) -> Unit,
    modifier: Modifier = Modifier
) {
    var isEditing by remember { mutableStateOf(false) }
    var editedName by remember { mutableStateOf(folderName) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .fillMaxHeight(0.7f),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header with folder name
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isEditing) {
                            OutlinedTextField(
                                value = editedName,
                                onValueChange = { editedName = it },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                textStyle = MaterialTheme.typography.titleLarge
                            )
                            IconButton(
                                onClick = {
                                    onRename(editedName)
                                    isEditing = false
                                }
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Done")
                            }
                        } else {
                            Text(
                                text = folderName.ifEmpty { "Unnamed" },
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { isEditing = true }) {
                                Icon(Icons.Default.Edit, contentDescription = "Rename")
                            }
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.Default.Close, contentDescription = "Close")
                            }
                        }
                    }
                }

                Divider()

                // Apps grid
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(apps, key = { it.packageName }) { app ->
                        AppIcon(
                            app = app,
                            onClick = { onAppClick(app) },
                            size = 56.dp,
                            showLabel = true
                        )
                    }
                }
            }
        }
    }
}

/**
 * Dialog for creating a new folder
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateFolderDialog(
    apps: List<AppInfo>,
    onDismiss: () -> Unit,
    onCreate: (String, List<AppInfo>) -> Unit
) {
    var folderName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Folder") },
        text = {
            Column {
                OutlinedTextField(
                    value = folderName,
                    onValueChange = { folderName = it },
                    label = { Text("Folder Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "${apps.size} apps selected",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onCreate(folderName, apps)
                    onDismiss()
                },
                enabled = apps.size >= 2
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
