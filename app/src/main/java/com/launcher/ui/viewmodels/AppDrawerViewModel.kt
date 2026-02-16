package com.launcher.ui.viewmodels

import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.launcher.data.models.AppInfo
import com.launcher.extensions.ExtensionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AppDrawerViewModel(application: Application) : AndroidViewModel(application) {
    private val packageManager = application.packageManager
    private val extensionManager = ExtensionManager(application)

    private val _allApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val allApps: StateFlow<List<AppInfo>> = _allApps.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _filteredApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val filteredApps: StateFlow<List<AppInfo>> = _filteredApps.asStateFlow()

    init {
        loadInstalledApps()
        notifyDrawerOpened()
    }

    private fun notifyDrawerOpened() {
        viewModelScope.launch(Dispatchers.IO) {
            // Load extensions
            extensionManager.loadInstalledExtensions()

            // Notify drawer opened
            extensionManager.onAppDrawerOpened()
        }
    }

    fun notifyAppLaunched(packageName: String) {
        extensionManager.onAppLaunched(packageName)
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        filterApps(query)
    }

    private fun filterApps(query: String) {
        viewModelScope.launch(Dispatchers.Default) {
            val filtered = if (query.isBlank()) {
                _allApps.value
            } else {
                _allApps.value
                    .map { app -> app to app.calculateSearchScore(query) }
                    .filter { it.second > 0.3f }
                    .sortedByDescending { it.second }
                    .map { it.first }
            }
            _filteredApps.value = filtered
        }
    }

    private fun loadInstalledApps() {
        viewModelScope.launch(Dispatchers.IO) {
            val apps = getInstalledApps()
            _allApps.value = apps
            _filteredApps.value = apps
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
}
