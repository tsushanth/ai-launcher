package com.launcher.ui.viewmodels

import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.launcher.data.models.AppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeScreenViewModel(application: Application) : AndroidViewModel(application) {
    private val packageManager = application.packageManager

    private val _dockApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val dockApps: StateFlow<List<AppInfo>> = _dockApps.asStateFlow()

    private val _allApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val allApps: StateFlow<List<AppInfo>> = _allApps.asStateFlow()

    init {
        loadInstalledApps()
    }

    fun loadInstalledApps() {
        viewModelScope.launch(Dispatchers.IO) {
            val apps = getInstalledApps()
            _allApps.value = apps

            // Set default dock apps (first 5 apps alphabetically for now)
            // TODO: Load from user preferences
            _dockApps.value = apps.take(5)
        }
    }

    private fun getInstalledApps(): List<AppInfo> {
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val apps = packageManager.queryIntentActivities(intent, PackageManager.MATCH_ALL)
            .mapNotNull { resolveInfo ->
                try {
                    val packageName = resolveInfo.activityInfo.packageName
                    val appName = resolveInfo.loadLabel(packageManager).toString()
                    val icon = resolveInfo.loadIcon(packageManager)
                    val appInfo = packageManager.getApplicationInfo(packageName, 0)
                    val isSystemApp = (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0

                    AppInfo(
                        packageName = packageName,
                        name = appName,
                        icon = icon,
                        isSystemApp = isSystemApp,
                        installTime = packageManager.getPackageInfo(packageName, 0).firstInstallTime
                    )
                } catch (e: Exception) {
                    null
                }
            }
            .sortedBy { it.name.lowercase() }

        return apps
    }

    fun updateDockApps(apps: List<AppInfo>) {
        _dockApps.value = apps.take(5)
        // TODO: Save to preferences
    }
}
