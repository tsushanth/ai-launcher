package com.launcher.utils

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

class LauncherWallpaperManager(private val context: Context) {
    private val wallpaperManager = WallpaperManager.getInstance(context)

    /**
     * Set static wallpaper from URI
     */
    suspend fun setStaticWallpaper(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val bitmap = context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it)
            } ?: return@withContext Result.failure(IOException("Failed to load image"))

            wallpaperManager.setBitmap(bitmap)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Launch live wallpaper picker
     */
    fun pickLiveWallpaper(launcher: ActivityResultLauncher<Intent>) {
        val intent = Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER)
        launcher.launch(intent)
    }

    /**
     * Set specific live wallpaper
     */
    fun setLiveWallpaper(component: ComponentName) {
        val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
            putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, component)
        }
        context.startActivity(intent)
    }

    /**
     * Clear wallpaper (set to default)
     */
    suspend fun clearWallpaper(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            wallpaperManager.clear()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get current wallpaper drawable
     */
    fun getCurrentWallpaper() = wallpaperManager.drawable

    /**
     * Check if device supports live wallpapers
     */
    fun supportsLiveWallpapers(): Boolean {
        return wallpaperManager.isWallpaperSupported && wallpaperManager.isSetWallpaperAllowed
    }
}
