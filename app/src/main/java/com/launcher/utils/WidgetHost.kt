package com.launcher.utils

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import androidx.activity.result.ActivityResultLauncher

class LauncherWidgetHost(private val context: Context) {
    companion object {
        private const val APPWIDGET_HOST_ID = 1024
    }

    private val appWidgetManager = AppWidgetManager.getInstance(context)
    private val widgetHost = AppWidgetHost(context, APPWIDGET_HOST_ID)

    fun startListening() {
        try {
            widgetHost.startListening()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stopListening() {
        try {
            widgetHost.stopListening()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Allocate a new widget ID
     */
    fun allocateAppWidgetId(): Int {
        return widgetHost.allocateAppWidgetId()
    }

    /**
     * Delete widget by ID
     */
    fun deleteAppWidgetId(appWidgetId: Int) {
        widgetHost.deleteAppWidgetId(appWidgetId)
    }

    /**
     * Create widget view for given widget ID
     */
    fun createView(appWidgetId: Int): AppWidgetHostView? {
        return try {
            val appWidgetInfo = appWidgetManager.getAppWidgetInfo(appWidgetId)
            val hostView = widgetHost.createView(context, appWidgetId, appWidgetInfo) as AppWidgetHostView
            hostView.setAppWidget(appWidgetId, appWidgetInfo)
            hostView
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Launch widget picker
     */
    fun pickWidget(launcher: ActivityResultLauncher<Intent>): Int {
        val appWidgetId = allocateAppWidgetId()
        val pickIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_PICK).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        launcher.launch(pickIntent)
        return appWidgetId
    }

    /**
     * Configure widget (if required)
     */
    fun configureWidget(
        appWidgetId: Int,
        provider: ComponentName,
        launcher: ActivityResultLauncher<Intent>
    ): Boolean {
        val canBind = appWidgetManager.bindAppWidgetIdIfAllowed(appWidgetId, provider)

        if (!canBind) {
            // Request permission to bind
            val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_BIND).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, provider)
            }
            launcher.launch(intent)
            return false
        }

        // Check if widget needs configuration
        val info = appWidgetManager.getAppWidgetInfo(appWidgetId)
        if (info.configure != null) {
            val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE).apply {
                component = info.configure
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            launcher.launch(intent)
            return false
        }

        return true
    }

    /**
     * Get widget info
     */
    fun getWidgetInfo(appWidgetId: Int): AppWidgetProviderInfo? {
        return try {
            appWidgetManager.getAppWidgetInfo(appWidgetId)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Get installed widgets
     */
    fun getInstalledWidgets(): List<AppWidgetProviderInfo> {
        return appWidgetManager.installedProviders
    }

    /**
     * Calculate widget size based on grid cells
     */
    fun calculateWidgetSize(rows: Int, cols: Int, cellSize: Int): Rect {
        val width = cols * cellSize
        val height = rows * cellSize
        return Rect(0, 0, width, height)
    }
}
