package com.launcher.data

import android.content.Context
import com.launcher.data.models.DesktopItem
import com.launcher.data.models.GridPosition
import com.launcher.data.models.HomeScreenLayout
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class LauncherRepository(context: Context) {
    private val database = LauncherDatabase.getDatabase(context)
    private val homeScreenDao = database.homeScreenDao()

    val homeScreenLayout: Flow<HomeScreenLayout> = homeScreenDao.getLayout().map { layout ->
        layout ?: getDefaultLayout()
    }

    suspend fun updateLayout(layout: HomeScreenLayout) {
        homeScreenDao.updateLayout(layout)
    }

    suspend fun insertLayout(layout: HomeScreenLayout) {
        homeScreenDao.insertLayout(layout)
    }

    suspend fun addAppToHomeScreen(packageName: String, position: GridPosition) {
        val currentLayout = homeScreenDao.getLayoutOnce() ?: getDefaultLayout()
        val newItem = DesktopItem.AppShortcut(packageName, position)
        val updatedItems = currentLayout.items + newItem
        homeScreenDao.updateLayout(currentLayout.copy(items = updatedItems))
    }

    suspend fun removeAppFromHomeScreen(packageName: String) {
        val currentLayout = homeScreenDao.getLayoutOnce() ?: return
        val updatedItems = currentLayout.items.filterNot {
            it is DesktopItem.AppShortcut && it.packageName == packageName
        }
        homeScreenDao.updateLayout(currentLayout.copy(items = updatedItems))
    }

    suspend fun createFolder(name: String, apps: List<String>, position: GridPosition) {
        val currentLayout = homeScreenDao.getLayoutOnce() ?: getDefaultLayout()
        val folderId = UUID.randomUUID().toString()
        val newFolder = DesktopItem.Folder(
            id = folderId,
            name = name,
            apps = apps,
            position = position
        )

        // Remove individual app shortcuts that are now in the folder
        val updatedItems = currentLayout.items.filterNot {
            it is DesktopItem.AppShortcut && apps.contains(it.packageName)
        } + newFolder

        homeScreenDao.updateLayout(currentLayout.copy(items = updatedItems))
    }

    suspend fun addAppToFolder(folderId: String, packageName: String) {
        val currentLayout = homeScreenDao.getLayoutOnce() ?: return
        val updatedItems = currentLayout.items.map { item ->
            if (item is DesktopItem.Folder && item.id == folderId) {
                item.copy(apps = item.apps + packageName)
            } else {
                item
            }
        }
        homeScreenDao.updateLayout(currentLayout.copy(items = updatedItems))
    }

    suspend fun removeAppFromFolder(folderId: String, packageName: String) {
        val currentLayout = homeScreenDao.getLayoutOnce() ?: return
        val updatedItems = currentLayout.items.mapNotNull { item ->
            if (item is DesktopItem.Folder && item.id == folderId) {
                val updatedApps = item.apps - packageName
                // Remove folder if it has less than 2 apps
                if (updatedApps.size < 2) null
                else item.copy(apps = updatedApps)
            } else {
                item
            }
        }
        homeScreenDao.updateLayout(currentLayout.copy(items = updatedItems))
    }

    suspend fun renameFolder(folderId: String, newName: String) {
        val currentLayout = homeScreenDao.getLayoutOnce() ?: return
        val updatedItems = currentLayout.items.map { item ->
            if (item is DesktopItem.Folder && item.id == folderId) {
                item.copy(name = newName)
            } else {
                item
            }
        }
        homeScreenDao.updateLayout(currentLayout.copy(items = updatedItems))
    }

    suspend fun updateDockApps(packageNames: List<String>) {
        val currentLayout = homeScreenDao.getLayoutOnce() ?: getDefaultLayout()
        homeScreenDao.updateLayout(currentLayout.copy(dockApps = packageNames.take(5)))
    }

    private fun getDefaultLayout() = HomeScreenLayout(
        id = 1,
        gridRows = 5,
        gridCols = 4,
        items = emptyList(),
        dockApps = emptyList()
    )
}
