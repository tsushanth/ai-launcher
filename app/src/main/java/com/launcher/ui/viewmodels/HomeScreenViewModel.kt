package com.launcher.ui.viewmodels

import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.launcher.data.LauncherRepository
import com.launcher.data.models.AppInfo
import com.launcher.data.models.DesktopItem
import com.launcher.data.models.GridPosition
import com.launcher.data.models.HomeScreenLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class HomeScreenViewModel(application: Application) : AndroidViewModel(application) {
    private val packageManager = application.packageManager
    private val repository = LauncherRepository(application)

    // All installed apps
    private val _allApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val allApps: StateFlow<List<AppInfo>> = _allApps.asStateFlow()

    // Home screen layout from database
    val homeScreenLayout: StateFlow<HomeScreenLayout> = repository.homeScreenLayout
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = HomeScreenLayout()
        )

    // Dock apps (derived from layout)
    val dockApps: StateFlow<List<AppInfo>> = homeScreenLayout.map { layout ->
        layout.dockApps.mapNotNull { packageName ->
            _allApps.value.find { it.packageName == packageName }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Desktop items (folders, app shortcuts, widgets)
    val desktopItems: StateFlow<List<DesktopItem>> = homeScreenLayout.map { it.items }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        loadInstalledApps()
        initializeDefaultLayout()
    }

    fun loadInstalledApps() {
        viewModelScope.launch(Dispatchers.IO) {
            val apps = getInstalledApps()
            _allApps.value = apps
        }
    }

    private fun initializeDefaultLayout() {
        viewModelScope.launch(Dispatchers.IO) {
            val currentLayout = repository.homeScreenLayout.first()

            // If layout is empty, initialize with default dock apps
            if (currentLayout.dockApps.isEmpty()) {
                val defaultDockApps = _allApps.value.take(5).map { it.packageName }
                repository.updateDockApps(defaultDockApps)
            }
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

    // Add app to home screen
    fun addAppToHomeScreen(packageName: String, position: GridPosition) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.addAppToHomeScreen(packageName, position)
        }
    }

    // Remove app from home screen
    fun removeAppFromHomeScreen(packageName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.removeAppFromHomeScreen(packageName)
        }
    }

    // Create folder
    fun createFolder(name: String, apps: List<String>, position: GridPosition) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.createFolder(name, apps, position)
        }
    }

    // Add app to folder
    fun addAppToFolder(folderId: String, packageName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.addAppToFolder(folderId, packageName)
        }
    }

    // Remove app from folder
    fun removeAppFromFolder(folderId: String, packageName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.removeAppFromFolder(folderId, packageName)
        }
    }

    // Rename folder
    fun renameFolder(folderId: String, newName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.renameFolder(folderId, newName)
        }
    }

    // Update dock apps
    fun updateDockApps(packageNames: List<String>) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateDockApps(packageNames)
        }
    }

    // Get app info by package name
    fun getAppInfo(packageName: String): AppInfo? {
        return _allApps.value.find { it.packageName == packageName }
    }

    // Get apps for folder
    fun getAppsForFolder(folder: DesktopItem.Folder): List<AppInfo> {
        return folder.apps.mapNotNull { packageName ->
            getAppInfo(packageName)
        }
    }
}
