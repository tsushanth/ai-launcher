package com.launcher.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

@Entity(tableName = "home_screen_layouts")
data class HomeScreenLayout(
    @PrimaryKey val id: Int = 1, // Single home screen for now
    val gridRows: Int = 5,
    val gridCols: Int = 4,
    val items: List<DesktopItem> = emptyList(),
    val dockApps: List<String> = emptyList() // Package names
)

sealed class DesktopItem {
    abstract val position: GridPosition

    data class AppShortcut(
        val packageName: String,
        override val position: GridPosition
    ) : DesktopItem()

    data class Folder(
        val id: String,
        val name: String,
        val apps: List<String>, // Package names
        override val position: GridPosition
    ) : DesktopItem()

    data class Widget(
        val widgetId: Int,
        override val position: GridPosition,
        val size: GridSize
    ) : DesktopItem()
}

data class GridPosition(
    val row: Int,
    val col: Int
) {
    fun isValid(maxRows: Int, maxCols: Int): Boolean {
        return row in 0 until maxRows && col in 0 until maxCols
    }
}

data class GridSize(
    val rows: Int,
    val cols: Int
)

// Type converters for Room
class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromDesktopItemList(value: List<DesktopItem>): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toDesktopItemList(value: String): List<DesktopItem> {
        val listType = object : TypeToken<List<DesktopItem>>() {}.type
        return gson.fromJson(value, listType)
    }

    @TypeConverter
    fun fromStringList(value: List<String>): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toStringList(value: String): List<String> {
        val listType = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(value, listType)
    }
}
