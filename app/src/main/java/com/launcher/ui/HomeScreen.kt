package com.launcher.ui

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
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
import com.launcher.ui.viewmodels.HomeScreenViewModel

@Composable
fun HomeScreen(
    viewModel: HomeScreenViewModel = viewModel(),
    onOpenAppDrawer: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val context = LocalContext.current
    val dockApps by viewModel.dockApps.collectAsState()

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

            Spacer(Modifier.weight(1f))

            // Dock at bottom
            Dock(
                apps = dockApps,
                onAppClick = { app ->
                    val intent = context.packageManager.getLaunchIntentForPackage(app.packageName)
                    intent?.let { context.startActivity(it) }
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

@Composable
fun Dock(
    apps: List<AppInfo>,
    onAppClick: (AppInfo) -> Unit,
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
                    size = 56.dp,
                    showLabel = false
                )
            }
        }
    }
}

@Composable
fun AppIcon(
    app: AppInfo,
    onClick: () -> Unit,
    size: kotlin.dp = 60.dp,
    showLabel: Boolean = true,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(size + 16.dp)
            .clickable(onClick = onClick),
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
